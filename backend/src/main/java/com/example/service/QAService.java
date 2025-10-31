package com.example.service;

import com.example.mapper.mysql.QAMapper;
import jakarta.annotation.Resource;
import org.springframework.http.*;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Service
public class QAService {

    private static final String API_URL = "http://localhost:11434/v1/chat/completions";
    private static final String API_KEY = "";

    // 表结构信息：可以硬编码或配置
    private static final String TABLE_PROMPT = """
            我现在有以下四张表，请根据我的问题生成对应的 SQL 查询语句：
            如果有类似总结的需求，可以直接跳过，只需要满足后边的数据查询需求即可，如“可以帮我总结下最近一条部门通知的内容吗”只需要返回最近一条的部门通知
            请注意!你只需要生成sql，除了sql语句什么都不要生成，包括你的任何话，并且sql不要有任何换行
            如果无法满足用户要求，输出”数据库无相关结果“三个字
            表一：员工(employee)表
            字段： emp_no, user_name, role, birth_date, first_name, last_name, gender, dept_no(对应 dept_name:1监察部门，2财务部门，3后勤部门，4运维部门，5开发部门，6产品部门，7测试部门，8人力部门，9市场部门，10销售部门), hire_date, level
            表二：部门(department)表
            字段：dept_no, dept_name
            表三：部门通知表(article)
            字段：id,title(标题),description(简介),content(内容),time(发布日期)
            表四：部门收入统计 (department_revenue) 表
            字段：dept_no(部门编号), month(如2024-01-01), revenue(实际收入), target_revenue(目标收入), achieved_rate(达成率，revenue ÷ target_revenue)
            """ + "\n我的问题是：" + "%s";

    private final RestTemplate restTemplate = new RestTemplate();

    public void askStream(List<Map<String, String>> messages, SseEmitter emitter) {
        // 构造请求体
        Map<String, Object> data = Map.of(
                "model", "qwen3:1.7b",
                "messages", messages,
                "stream", true
        );

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", API_KEY);
        headers.setContentType(MediaType.APPLICATION_JSON);

        restTemplate.execute(
                API_URL,
                HttpMethod.POST,
                request -> {
                    request.getHeaders().putAll(headers);
                    new MappingJackson2HttpMessageConverter()
                            .write(data, MediaType.APPLICATION_JSON, request);
                },
                response -> {
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
                    }
                    return null;
                }
        );
    }

    @Resource
    private QAMapper qaMapper;

    public String executeGeneratedSql(String question) {
        String sql = generateSql(question); // 调用现有的生成 SQL 方法

        if (sql == null || !sql.trim().toLowerCase().startsWith("select")) {
            throw new IllegalArgumentException("只允许执行 SELECT 查询语句");
        }

        List<Map<String, Object>> result = qaMapper.executeSql(sql);

        // 如果查询结果不为空，则遍历所有行并拼接
        if (result != null && !result.isEmpty()) {
            StringBuilder resultStr = new StringBuilder();

            for (Map<String, Object> row : result) {
                for (Map.Entry<String, Object> entry : row.entrySet()) {
                    resultStr.append(entry.getKey())
                            .append(" = ")
                            .append(entry.getValue())
                            .append("\n");
                }
                resultStr.append("\n");
            }

            return resultStr.toString(); // 返回拼接好的字符串
        }

        return "没有查询结果";
    }

    public String generateSql(String question) {
        // 拼接表结构提示
        String prompt = String.format(TABLE_PROMPT, question);

        // 构造请求体
        Map<String, Object> data = Map.of(
                "model", "qwen3:1.7b",
                "messages", List.of(Map.of("role", "user", "content", "/nothink" + prompt)),
                "stream", false
        );

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", API_KEY);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(data, headers);
        ResponseEntity<Map> resp = restTemplate.postForEntity(API_URL, entity, Map.class);

        if (resp.getStatusCode() == HttpStatus.OK && resp.getBody() != null) {
            List<Map<String, Object>> choices = (List<Map<String, Object>>) resp.getBody().get("choices");
            if (choices != null && !choices.isEmpty()) {
                Map<String, Object> first = choices.get(0);
                Map<String, Object> message = (Map<String, Object>) first.get("message");
                String content = (String) message.get("content");

                if (content != null) {
                    // 删除前两行 <think></think> 标签
                    content = content.replaceFirst("(?s)^(\\s*<think>.*?</think>\\s*){1,2}", "");

                    // 使用正则提取 ```sql 或 ``` 包裹的内容
                    String regex = "```(?:sql)?\\s*([\\s\\S]*?)\\s*```";
                    java.util.regex.Matcher matcher = java.util.regex.Pattern.compile(regex).matcher(content);
                    if (matcher.find()) {
                        return matcher.group(1).trim();
                    } else {
                        // 如果没有```包裹，返回原始内容
                        return content.trim();
                    }
                }
            }
        }
        return null;
    }

    public String normalAsk(List<Map<String, String>> question, SseEmitter emitter) {
        Map<String, Object> data = Map.of(
                "model", "4.0Ultra",
                "messages", question,
                "stream", true
        );

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", API_KEY);
        headers.setContentType(MediaType.APPLICATION_JSON);

        restTemplate.execute(
                API_URL,
                HttpMethod.POST,
                request -> {
                    request.getHeaders().putAll(headers);
                    new MappingJackson2HttpMessageConverter()
                            .write(data, MediaType.APPLICATION_JSON, request);
                },
                response -> {
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
                    }
                    return null;
                }
        );
        return null;
    }
}

