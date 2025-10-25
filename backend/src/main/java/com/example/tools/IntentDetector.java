package com.example.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.model.ChatRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;
import org.springframework.core.io.ClassPathResource;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class IntentDetector {
    private static final String QWEN_URL = "http://localhost:11434/v1/chat/completions";
    private static final RestTemplate restTemplate = new RestTemplate();

    public static ToolIntent detect(ChatRequest request) {
        try {
            // 只取最后一条用户消息
            ChatRequest tempReq = new ChatRequest();
            tempReq.setModel(request.getModel());
            tempReq.setStream(false);
            tempReq.setTool_choice("auto");
            // 添加系统提示词
            List<Map<String, String>> newMessages = new ArrayList<>();
            // 添加系统提示词
            newMessages.add(Map.of(
                    "role", "system",
                    "content", "你是一个意图识别助手，你的任务是判断用户的意图是否需要调用工具函数。" +
                            "如果用户的问题可以通过工具函数解决（如查询天气、股票），请直接调用对应的工具函数。" +
                            "如果不需要工具函数，请直接返回'不需要工具函数'，不要解释原因，不要有其他回复。" +
                            "记住：你的回复要么是工具调用，要么就是简单的'不需要工具函数'。"
            ));
            // 添加全部历史消息（用户+助手的对话）
            newMessages.addAll(request.getMessages());

            tempReq.setMessages(newMessages);
            // 读取tools.json
            ClassPathResource resource = new ClassPathResource("tools.json");
            String toolsJson = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            ObjectMapper mapper = new ObjectMapper();
            tempReq.setTools(mapper.readValue(toolsJson, List.class));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            StringBuilder responseStr = new StringBuilder();
            restTemplate.execute(
                    QWEN_URL,
                    HttpMethod.POST,
                    req -> {
                        req.getHeaders().putAll(headers);
                        new MappingJackson2HttpMessageConverter().write(tempReq, MediaType.APPLICATION_JSON, req);
                    },
                    response -> {
                        responseStr.append(new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8));
                        return null;
                    }
            );
            String resp = responseStr.toString();
            System.out.println("[IntentDetector] QWEN工具意图识别返回: " + resp);
            JsonNode node = mapper.readTree(resp);
            // 如果返回"不需要工具函数"，直接返回null
            String content = node.at("/choices/0/message/content").asText("");
            if (content.contains("不需要工具函数")) {
                return null;
            }
            // 兼容function_call和tool_calls两种格式
            JsonNode functionCall = node.at("/choices/0/message/function_call");
            if (!functionCall.isMissingNode() && functionCall.has("name")) {
                String funcName = functionCall.get("name").asText();
                String arguments = functionCall.get("arguments").toString();
                return new ToolIntent(funcName, arguments);
            }
            JsonNode toolCalls = node.at("/choices/0/message/tool_calls");
            if (toolCalls.isArray() && toolCalls.size() > 0) {
                JsonNode function = toolCalls.get(0).get("function");
                if (function != null && function.has("name")) {
                    String funcName = function.get("name").asText();
                    String arguments = function.get("arguments").asText();
                    return new ToolIntent(funcName, arguments);
                }
            }
        } catch (Exception e) {
            System.out.println("[IntentDetector] QWEN工具意图识别异常: " + e.getMessage());
        }
        return null;
    }

    public static class ToolIntent {
        public final String name;
        public final String arguments;
        public ToolIntent(String name, String arguments) {
            this.name = name;
            this.arguments = arguments;
        }
    }
} 