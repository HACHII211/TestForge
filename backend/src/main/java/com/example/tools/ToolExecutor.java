package com.example.tools;

import com.example.model.ChatRequest;
import com.example.schema.SchemaHolder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

@Component
public class ToolExecutor {
    private static final ObjectMapper mapper = new ObjectMapper();

    private final JdbcTemplate jdbcTemplate;
    private final SchemaHolder schemaHolder;
    private final RestTemplate restTemplate;   // 可保留用于 ToolExecutor 自身需要
    private final ToolFunctions toolFunctions; // 注入的组件（实例方法）

    public ToolExecutor(JdbcTemplate jdbcTemplate,
                        SchemaHolder schemaHolder,
                        RestTemplate restTemplate,
                        ToolFunctions toolFunctions) {
        this.jdbcTemplate = jdbcTemplate;
        this.schemaHolder = schemaHolder;
        this.restTemplate = restTemplate;
        this.toolFunctions = toolFunctions;
    }

    /**
     * 根据 toolName 路由到不同工具，execute_sql 分支会调用 toolFunctions.executeSqlAndFollowUp(...)
     */
    public void execute(String toolName, String arguments, ChatRequest request, SseEmitter emitter) {
        System.out.println("[ToolExecutor] 开始执行: " + toolName + ", args: " + arguments);
        try {
            sendLiveStatus(emitter, "thinking...");

            String result = "";

            switch (toolName) {
                case "generate_test_cases": {
                    JsonNode argNode = mapper.readTree(arguments).get("test_cases");
                    String inputArrayJson = mapper.writeValueAsString(argNode);
                    result = toolFunctions.generateTestCases(inputArrayJson);
                    break;
                }
                case "txt2sql": {
                    // 如果你仍会在 tools.json 中触发 txt2sql，IntentDetector 理想情况下已经把 SQL 生成好并返回 execute_sql
                    // 这里保留占位（不直接处理）
                    break;
                }
                case "execute_sql": {
                   JsonNode argNode = mapper.readTree(arguments);
                    String sql = argNode.path("sql").asText("");
                    JsonNode paramsNode = argNode.path("params");
                    List<Object> params = mapper.convertValue(paramsNode, List.class);

                    toolFunctions.executeSqlAndFollowUp(sql, params, request, jdbcTemplate, schemaHolder, emitter);
                    return;
                }
                default:
                    throw new UnsupportedOperationException("未知工具: " + toolName);
            }

            // 非 SQL 工具走这里返回结果
            sendLiveStatus(emitter, "");
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
            e.printStackTrace();
            sendLiveStatus(emitter, "");
            try {
                emitter.completeWithError(e);
            } catch (Exception ignore) {}
        }
    }

    // Helper method to send live status updates
    private static void sendLiveStatus(SseEmitter emitter, String statusMessage) {
        try {
            String jsonStatus = mapper.writeValueAsString(Map.of("status_message", statusMessage));
            emitter.send(jsonStatus + "\n", MediaType.TEXT_EVENT_STREAM);
        } catch (Exception e) {
            System.err.println("Error sending live status from ToolExecutor: " + e.getMessage());
        }
    }
}
