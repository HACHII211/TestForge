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
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final RestTemplate restTemplate;
    private final IntentDetector intentDetector;   // 注入改造后的 IntentDetector Bean
    private final ToolExecutor toolExecutor;       // 注入改造后的 ToolExecutor Bean

    public void chat(ChatRequest request, SseEmitter emitter) {
        try {
            System.out.println(request.getTool_choice());
            if (request.getTool_choice().equals("testCase")) {
                // 发送 planning 状态
                sendLiveStatus(emitter, "planing...");

                // 1. 意图识别（保持你原来的流程，但 IntentDetector 现在可以做扩展性的 SQL 两步）
                IntentDetector.ToolIntent toolIntent = intentDetector.detect(request);
                System.out.println("[ChatService] IntentDetector.detect 返回: " + (toolIntent == null ? null : toolIntent.name));
                if (toolIntent != null) {
                    System.out.println("[ChatService] 进入工具调用分支: " + toolIntent.name + ", args: " + toolIntent.arguments);
                    // ToolExecutor 会负责把结果通过 SSE 发回，并在需要时继续向 QWEN 发起后续对话（并把结果流回）
                    toolExecutor.execute(toolIntent.name, toolIntent.arguments, request, emitter);
                    return;
                }
            }
            System.out.println("[ChatService] 意图为null，进入流式对话分支");
            sendLiveStatus(emitter, "thinking...");

        } catch (Exception e) {
            System.out.println("[ChatService] IntentDetector 检测失败: " + e.getMessage());
            sendLiveStatus(emitter, "");
        }

        // 如果没有工具调用，按原样把请求转发给 QWEN 并把流式响应发回
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
            String jsonStatus = objectMapper.writeValueAsString(Map.of("status_message", statusMessage));
            emitter.send(jsonStatus + "\n", MediaType.TEXT_EVENT_STREAM);
        } catch (Exception e) {
            System.err.println("Error sending live status: " + e.getMessage());
        }
    }
}
