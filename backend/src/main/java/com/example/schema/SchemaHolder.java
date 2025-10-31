package com.example.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Component
public class SchemaHolder {
    private final ObjectMapper mapper = new ObjectMapper();
    private Map<String, JsonNode> tableSchemas = new HashMap<>();

    @PostConstruct
    public void init() throws Exception {
        ClassPathResource res = new ClassPathResource("schema.json");
        String text = new String(res.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        JsonNode root = mapper.readTree(text);
        Iterator<String> it = root.fieldNames();
        while (it.hasNext()) {
            String table = it.next();
            tableSchemas.put(table.toLowerCase(), root.get(table));
        }
    }

    public Set<String> listTableNames() {
        return Collections.unmodifiableSet(tableSchemas.keySet());
    }

    public JsonNode getTableSchema(String tableName) {
        return tableSchemas.get(tableName.toLowerCase());
    }

    // 返回用于发送给模型的精简 schema（只选列名/type/nullable/pk/desc）
    public String getCompactSchemaForTables(List<String> tables) throws Exception {
        ObjectMapper m = mapper;
        Map<String,Object> out = new LinkedHashMap<>();
        for (String t : tables) {
            JsonNode s = getTableSchema(t);
            if (s == null) continue;
            // 构造精简版
            Map<String,Object> small = new LinkedHashMap<>();
            small.put("table_name", t);
            small.put("description", s.path("description").asText(""));
            List<Map<String,Object>> cols = new ArrayList<>();
            for (JsonNode c : s.path("columns")) {
                Map<String,Object> col = new LinkedHashMap<>();
                col.put("name", c.path("name").asText());
                col.put("type", c.path("type").asText());
                col.put("pk", c.path("pk").asBoolean(false));
                col.put("nullable", c.path("nullable").asBoolean(true));
                col.put("desc", c.path("desc").asText(""));
                cols.add(col);
            }
            small.put("columns", cols);
            out.put(t, small);
        }
        return m.writeValueAsString(out);
    }
}
