package com.example.tools;

import com.example.schema.SchemaHolder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.util.TablesNamesFinder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * ToolFunctions 现在是一个 Spring @Component（单例）；
 * 提供实例方法，线程安全（不持有可变共享状态），并使用 Semaphore 做并发保护。
 */
@Component
public class ToolFunctions {

    private final ObjectMapper mapper;
    private final RestTemplate restTemplate;
    private final int sqlConcurrencyLimit;
    private final Semaphore sqlSemaphore;
    private final long semaphoreTimeoutSeconds;
    private static final String QWEN_URL = "http://localhost:11434/v1/chat/completions";

    public ToolFunctions(ObjectMapper mapper,
                         RestTemplate restTemplate,
                         @Value("${app.sql.concurrent:10}") int sqlConcurrencyLimit,
                         @Value("${app.sql.semaphoreTimeoutSeconds:30}") long semaphoreTimeoutSeconds) {
        this.mapper = mapper;
        this.restTemplate = restTemplate;
        this.sqlConcurrencyLimit = Math.max(1, sqlConcurrencyLimit);
        this.semaphoreTimeoutSeconds = Math.max(5, semaphoreTimeoutSeconds);
        this.sqlSemaphore = new Semaphore(this.sqlConcurrencyLimit, true);
    }

    public String generateTestCase(String testCaseJson) throws Exception {
        JsonNode node = mapper.readTree(testCaseJson);
        if (!node.isObject()) {
            throw new IllegalArgumentException("输入必须是 JSON 对象");
        }
        ((com.fasterxml.jackson.databind.node.ObjectNode) node).put("actual_result", "");
        return mapper.writeValueAsString(node);
    }

    public String generateTestCases(String testCasesJson) throws Exception {
        JsonNode root = mapper.readTree(testCasesJson);
        if (!root.isArray()) {
            throw new IllegalArgumentException("输入必须是 JSON 数组");
        }
        for (JsonNode item : root) {
            if (item.isObject()) {
                ((com.fasterxml.jackson.databind.node.ObjectNode) item).put("actual_result", "");
            }
        }
        return mapper.writeValueAsString(root);
    }

    /**
     * 主要方法：校验 SQL -> 执行 -> 把结果发回前端并把结果追加进对话，调用 QWEN 做后续回复（流式）。
     *
     * 并发控制：使用 Semaphore.tryAcquire(timeout)，获取失败将返回错误给前端，避免无限阻塞。
     */
    @SuppressWarnings("unchecked")
    public void executeSqlAndFollowUp(String sql,
                                      List<Object> params,
                                      com.example.model.ChatRequest originalRequest,
                                      JdbcTemplate jdbcTemplate,
                                      SchemaHolder schemaHolder,
                                      SseEmitter emitter) {
        boolean acquired = false;
        try {
            // 尝试获取 semaphore（带超时），避免过多并发导致 DB 或 QWEN 压垮
            acquired = sqlSemaphore.tryAcquire(semaphoreTimeoutSeconds, TimeUnit.SECONDS);
            if (!acquired) {
                String err = mapper.writeValueAsString(Map.of("error", "当前系统忙，请稍后重试（并发限流）"));
                emitter.send(err + "\n", MediaType.TEXT_EVENT_STREAM);
                emitter.complete();
                return;
            }

            // 1) 语法校验（只允许 SELECT）
            Statement stmt = CCJSqlParserUtil.parse(sql);
            if (!(stmt instanceof Select)) {
                throw new SecurityException("仅允许 SELECT 查询");
            }

            // 2) 表名白名单校验
            TablesNamesFinder finder = new TablesNamesFinder();
            List<String> tables = finder.getTableList(stmt);
            var allowed = schemaHolder.listTableNames();
            for (String t : tables) {
                if (!allowed.contains(t.toLowerCase())) {
                    throw new SecurityException("SQL 中包含未授权表: " + t);
                }
            }

            // 3) 执行 SQL（参数化）
            List<Map<String, Object>> rows;
            if (params == null || params.isEmpty()) {
                rows = jdbcTemplate.queryForList(sql);
            } else {
                rows = jdbcTemplate.queryForList(sql, params.toArray());
            }

            // 4) 先把工具返回值发给前端（SSE）
            String resultJson = mapper.writeValueAsString(Map.of("rows", rows));
            String assistantMsg = mapper.writeValueAsString(Map.of(
                    "choices", List.of(
                            Map.of("delta", Map.of("content", resultJson))
                    )
            ));
            emitter.send("data: " + assistantMsg + "\n", MediaType.TEXT_EVENT_STREAM);

            // 5) 构造后续对话（把 resultJson 作为 assistant 内容），让 QWEN 生成自然语言摘要（流式）
            com.example.model.ChatRequest followReq = new com.example.model.ChatRequest();
            followReq.setModel(originalRequest.getModel());
            followReq.setStream(true);
            followReq.setTool_choice("none"); // 禁止再次自动调用工具，避免循环
            var messages = new java.util.ArrayList<Map<String, String>>();
            messages.addAll(originalRequest.getMessages());
            messages.add(Map.of("role", "assistant", "content", resultJson));
            messages.add(Map.of("role", "user", "content", "请基于上面查询结果，用简洁的中文给出结论和要点（不再调用工具）。"));
            followReq.setMessages(messages);

            // 6) 通过 restTemplate 发起流式请求，并转发到 SSE（与 ChatService 的做法一致）
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            restTemplate.execute(
                    QWEN_URL,
                    HttpMethod.POST,
                    req -> {
                        req.getHeaders().putAll(headers);
                        new MappingJackson2HttpMessageConverter().write(followReq, MediaType.APPLICATION_JSON, req);
                    },
                    response -> {
                        try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.getBody(), StandardCharsets.UTF_8))) {
                            String line;
                            while ((line = reader.readLine()) != null) {
                                line = line.trim();
                                if (line.isEmpty() || "data: [DONE]".equals(line)) {
                                    continue;
                                }
                                // 直接把模型的流式输出转发给 SSE
                                emitter.send(line + "\n", MediaType.TEXT_EVENT_STREAM);
                            }
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                        return null;
                    }
            );

            // 7) 完成
            emitter.complete();
        } catch (Exception e) {
            try {
                String err = mapper.writeValueAsString(Map.of("error", e.getMessage()));
                emitter.send(err + "\n", MediaType.TEXT_EVENT_STREAM);
            } catch (Exception ignore) { }
            emitter.completeWithError(e);
        } finally {
            if (acquired) {
                sqlSemaphore.release();
            }
        }
    }
}
