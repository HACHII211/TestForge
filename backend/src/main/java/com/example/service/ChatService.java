package com.example.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.model.ChatRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import com.example.tools.IntentDetector;
import com.example.tools.ToolExecutor;

@Service
@RequiredArgsConstructor
public class ChatService {
    private static final String QWEN_URL = "http://localhost:11434/v1/chat/completions";
    private final ObjectMapper objectMapper = new ObjectMapper(); // Initialize ObjectMapper

    private final RestTemplate restTemplate;

    public void chat(ChatRequest request, SseEmitter emitter) {
        try {
            // Send "planing..." status before intent detection
            sendLiveStatus(emitter, "planing...");

            // 1. 判断意图
            IntentDetector.ToolIntent toolIntent = IntentDetector.detect(request);
            System.out.println("[ChatService] IntentDetector.detect 返回: " + (toolIntent == null ? null : toolIntent.name));
            if (toolIntent != null) {
                System.out.println("[ChatService] 进入工具调用分支: " + toolIntent.name + ", args: " + toolIntent.arguments);
                ToolExecutor.execute(toolIntent.name, toolIntent.arguments, request, emitter);
                return;
            }
            System.out.println("[ChatService] 意图为null，进入流式对话分支");

            // Send "thinking..." status before sending to Qwen
            sendLiveStatus(emitter, "thinking...");

        } catch (Exception e) {
            System.out.println("[ChatService] IntentDetector 检测失败: " + e.getMessage());
            sendLiveStatus(emitter, ""); // Clear status on error
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        System.out.println("[ChatService] 发送请求到: " + QWEN_URL);
        System.out.println("[ChatService] 请求头: " + headers);

        restTemplate.execute(
                QWEN_URL,
                HttpMethod.POST,
                req -> {
                    req.getHeaders().putAll(headers);
                    System.out.println("[ChatService] 开始发送请求...");
                    new MappingJackson2HttpMessageConverter()
                            .write(request, MediaType.APPLICATION_JSON, req);
                },
                response -> {
                    System.out.println("[ChatService] 开始接收响应...");
                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(response.getBody(), StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            line = line.trim();
                            if (line.isEmpty() || "data: [DONE]".equals(line)) {
                                continue;
                            }
                            emitter.send(line + "\n", MediaType.TEXT_EVENT_STREAM);
                        }
                    } catch (Exception e) {
                        System.out.println("[ChatService] QWEN流式处理异常: " + e.getMessage());
                        e.printStackTrace();
                        emitter.completeWithError(e);
                    }
                    emitter.complete();
                    return null;
                }
        );
    }

    // Helper method to send live status updates
    private void sendLiveStatus(SseEmitter emitter, String statusMessage) {
        try {
            // Create a JSON object for the status message
            String jsonStatus = objectMapper.writeValueAsString(Map.of("status_message", statusMessage));
            emitter.send(jsonStatus + "\n", MediaType.TEXT_EVENT_STREAM);
        } catch (Exception e) {
            System.err.println("Error sending live status: " + e.getMessage());
        }
    }
}