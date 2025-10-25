package com.example.tools;

import com.example.model.ChatRequest;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.MediaType;

import java.util.List;
import java.util.Map;

public class ToolExecutor {
    private static final ObjectMapper mapper = new ObjectMapper();

    public static void execute(String toolName, String arguments, ChatRequest request, SseEmitter emitter) {
        System.out.println("[ToolExecutor] 开始执行: " + toolName + ", args: " + arguments + ", request: " + request);
        try {
            sendLiveStatus(emitter, "thinking..."); // Send thinking status before tool execution

            String result = "";

            switch (toolName) {
                case "get_weather": {
                    String city = mapper.readTree(arguments).get("city").asText();
                    System.out.println("[ToolExecutor] 参数 city: " + city);
                    result = SimpleToolFunctions.getWeather(city);
                    break;
                }
                case "get_stock_price": {
                    String ticker = mapper.readTree(arguments).get("ticker").asText();
                    System.out.println("[ToolExecutor] 参数 ticker: " + ticker);
                    result = SimpleToolFunctions.getStockPrice(ticker);
                    break;
                }
                case "generate_test_cases": {
                    // 批量用例
                    JsonNode argNode2 = mapper.readTree(arguments).get("test_cases");
                    String inputArrayJson = mapper.writeValueAsString(argNode2);
                    System.out.println("[ToolExecutor] 批量生成测试用例，原始 JSON 数组: " + inputArrayJson);
                    result = SimpleToolFunctions.generateTestCases(inputArrayJson);
                    System.out.println("[ToolExecutor] generateTestCases -> " + result);
                    break;
                }
                default:
                    throw new UnsupportedOperationException("未知工具: " + toolName);
            }

            // Clear status after tool execution and before sending final result
            sendLiveStatus(emitter, "");

            // 拼接 SSE 响应
            String assistantMsg = mapper.writeValueAsString(Map.of(
                    "choices", List.of(
                            Map.of("delta", Map.of("content", result))
                    )
            ));
            System.out.println("[ToolExecutor] 推送消息: " + assistantMsg);
            emitter.send("data: " + assistantMsg + "\n", MediaType.TEXT_EVENT_STREAM);
            emitter.complete();

        } catch (Exception e) {
            System.err.println("[ToolExecutor] 执行异常: " + e.getMessage());
            sendLiveStatus(emitter, ""); // Clear status on error
            try {
                emitter.completeWithError(e);
            } catch (Exception ignore) {}
        }
    }

    // Helper method to send live status updates
    private static void sendLiveStatus(SseEmitter emitter, String statusMessage) {
        try {
            // Create a JSON object for the status message
            String jsonStatus = mapper.writeValueAsString(Map.of("status_message", statusMessage));
            emitter.send(jsonStatus + "\n", MediaType.TEXT_EVENT_STREAM);
        } catch (Exception e) {
            System.err.println("Error sending live status from ToolExecutor: " + e.getMessage());
        }
    }
}
