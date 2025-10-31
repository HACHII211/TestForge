package com.example.tools;

import com.example.model.ChatRequest;
import com.example.schema.SchemaHolder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class IntentDetector {
    private static final String QWEN_URL = "http://localhost:11434/v1/chat/completions";
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();
    private final SchemaHolder schemaHolder; // 注入 schema holder

    public IntentDetector(SchemaHolder schemaHolder) {
        this.schemaHolder = schemaHolder;
    }

//    意图识别
    public ToolIntent detect(ChatRequest request) {
        try {
            ChatRequest tempReq = new ChatRequest();
            tempReq.setModel(request.getModel());
            tempReq.setStream(false);
            tempReq.setTool_choice("testCase");

            List<Map<String, String>> newMessages = new ArrayList<>();
            newMessages.add(Map.of(
                    "role", "system",
                    "content", "你是一个意图识别助手，你的任务是判断用户的意图是否需要调用工具函数。" +
                            "如果用户的问题可以通过工具函数解决（如生成登录相关的测试用例），请直接调用对应的工具函数。" +
                            "如果不需要工具函数，请直接返回'不需要工具函数'，不要解释原因，不要有其他回复。" +
                            "记住：你的回复要么是工具调用，要么就是简单的'不需要工具函数'。"
            ));
            List<Map<String, String>> originalMessages = request.getMessages();
            if (originalMessages != null && !originalMessages.isEmpty()) {
                Map<String, String> lastMessage = originalMessages.get(originalMessages.size() - 1);
                newMessages.add(lastMessage);
            }
            tempReq.setMessages(newMessages);
            System.out.println("[IntentDetector] 发送给大模型的对话为: " + tempReq);

            ClassPathResource resource = new ClassPathResource("tools.json");
            String toolsJson = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
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
            System.out.println("[IntentDetector] 第一步工具意图识别返回: " + resp);
            JsonNode node = mapper.readTree(resp);

            String content = node.at("/choices/0/message/content").asText("");
            if (content.contains("不需要工具函数")) {
                return null;
            }

            // 检查 function_call/tool_calls
            JsonNode functionCall = node.at("/choices/0/message/function_call");
            if (!functionCall.isMissingNode() && functionCall.has("name")) {
                String funcName = functionCall.get("name").asText();
                String arguments = functionCall.get("arguments").toString();

                // 如果检测到的工具是 SQL 生成工具（例如 "txt2sql"），我们在这里做两轮对话
                if ("txt2sql".equalsIgnoreCase(funcName) || "generate_sql".equalsIgnoreCase(funcName)) {
                    // 两轮逻辑：先给模型表名列表，要求返回需要的表名数组；然后把所选表的 compact schema 发给模型生成 SQL
                    return handleSqlTwoStep(request, arguments);
                }

                return new ToolIntent(funcName, arguments);
            }

            JsonNode toolCalls = node.at("/choices/0/message/tool_calls");
            if (toolCalls.isArray() && toolCalls.size() > 0) {
                JsonNode function = toolCalls.get(0).get("function");
                if (function != null && function.has("name")) {
                    String funcName = function.get("name").asText();
                    String arguments = function.get("arguments").asText();

                    if ("txt2sql".equalsIgnoreCase(funcName) || "generate_sql".equalsIgnoreCase(funcName)) {
                        return handleSqlTwoStep(request, arguments);
                    }

                    return new ToolIntent(funcName, arguments);
                }
            }
        } catch (Exception e) {
            System.out.println("[IntentDetector] 意图识别异常: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }


    private ToolIntent handleSqlTwoStep(ChatRequest originalRequest, String initialArgs) {
        try {
            // A. 先询问模型：你需要哪些表？（把表名列表提供给模型）
            List<Map<String, String>> prompt1 = new ArrayList<>();
            prompt1.add(Map.of("role", "system", "content",
                    "下面是数据库可用的表名列表（全部小写）。请仅返回一个 JSON 数组，包含你要使用的表名，例如 [\"user\",\"role\"]。如果不需要访问数据库，请返回字符串: 不需要工具函数。"));
            // 把历史对话也发过去，帮助模型理解需求
            prompt1.addAll(originalRequest.getMessages());

            ChatRequest req1 = new ChatRequest();
            req1.setModel(originalRequest.getModel());
            req1.setStream(false);
            req1.setTool_choice("none"); // 第二阶段我们自行控制
            req1.setMessages(prompt1);

            // 把表名加入 system prompt 作为额外上下文（附加）
            String tableListString = schemaHolder.listTableNames().toString();
            // 我们把它拼接到 tools prompt（简化实现）
            req1.getMessages().add(0, Map.of("role", "system", "content", "可用的表名: " + tableListString));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            StringBuilder resp1 = new StringBuilder();
            restTemplate.execute(
                    QWEN_URL,
                    HttpMethod.POST,
                    r -> {
                        r.getHeaders().putAll(headers);
                        new MappingJackson2HttpMessageConverter().write(req1, MediaType.APPLICATION_JSON, r);
                    },
                    response -> {
                        resp1.append(new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8));
                        return null;
                    }
            );
            String r1 = resp1.toString();
            System.out.println("[IntentDetector] 表选择响应: " + r1);
            JsonNode node1 = mapper.readTree(r1);
            String content1 = node1.at("/choices/0/message/content").asText("");
            if (content1.contains("不需要工具函数")) {
                return null;
            }

            // Parse JSON array of table names (attempt)
            List<String> selectedTables;
            try {
                JsonNode arr = mapper.readTree(content1);
                selectedTables = new ArrayList<>();
                if (arr.isArray()) {
                    for (JsonNode t : arr) {
                        selectedTables.add(t.asText().toLowerCase());
                    }
                } else {
                    // fallback: try extract words
                    selectedTables.add(content1.trim().toLowerCase());
                }
            } catch (Exception ex) {
                // 如果解析失败，尝试从文本中抽取表名（非常简单的策略）
                selectedTables = new ArrayList<>();
                for (String t : schemaHolder.listTableNames()) {
                    if (content1.toLowerCase().contains(t.toLowerCase())) {
                        selectedTables.add(t.toLowerCase());
                    }
                }
            }

            if (selectedTables.isEmpty()) {
                // 如果模型没有返回表名，放弃工具调用
                System.out.println("[IntentDetector] 未选中任何表，取消工具调用");
                return null;
            }

            // B. 使用 selectedTables 获取 compact schema，并请模型生成 SQL
            String compactSchema = schemaHolder.getCompactSchemaForTables(selectedTables);

            List<Map<String, String>> prompt2 = new ArrayList<>();
            prompt2.add(Map.of("role", "system", "content",
                    "下面是你可以使用的表结构（已精简，只包含列名、类型、主键信息、是否可为空、简短说明）。" +
                            "请基于这些表结构和用户需求生成 SQL。**严格要求**：只返回一个 JSON 对象，格式为 {\"sql\":\"...\", \"params\": [ ... ]}，或只返回 SQL 字符串。SQL 尽量使用参数化占位符 '?'，并且只使用上面列出的表。不要多说话。"));
            prompt2.add(Map.of("role", "system", "content", compactSchema));
            prompt2.addAll(originalRequest.getMessages()); // 历史对话

            ChatRequest req2 = new ChatRequest();
            req2.setModel(originalRequest.getModel());
            req2.setStream(false);
            req2.setTool_choice("none");
            req2.setMessages(prompt2);

            StringBuilder resp2 = new StringBuilder();
            restTemplate.execute(
                    QWEN_URL,
                    HttpMethod.POST,
                    r -> {
                        r.getHeaders().putAll(headers);
                        new MappingJackson2HttpMessageConverter().write(req2, MediaType.APPLICATION_JSON, r);
                    },
                    response -> {
                        resp2.append(new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8));
                        return null;
                    }
            );

            String r2 = resp2.toString();
            System.out.println("[IntentDetector] SQL 生成响应: " + r2);
            JsonNode node2 = mapper.readTree(r2);
            String content2 = node2.at("/choices/0/message/content").asText("").trim();

            // 尝试解析 JSON 格式 { "sql": "...", "params": [...] }
            try {
                JsonNode out = mapper.readTree(content2);
                if (out.has("sql")) {
                    String sql = out.get("sql").asText();
                    JsonNode paramsNode = out.path("params");
                    String finalArgs = mapper.writeValueAsString(Map.of("sql", sql, "params", paramsNode));
                    // 返回一个 execute_sql 的 ToolIntent
                    return new ToolIntent("execute_sql", finalArgs);
                }
            } catch (Exception ex) {
                // 不是 JSON，可能直接就是 SQL 字符串
            }

            // 如果不是 JSON，直接把 content2 作为 SQL 字符串
            String finalArgs = mapper.writeValueAsString(Map.of("sql", content2, "params", List.of()));
            return new ToolIntent("execute_sql", finalArgs);

        } catch (Exception e) {
            System.out.println("[IntentDetector] handleSqlTwoStep 异常: " + e.getMessage());
            e.printStackTrace();
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
