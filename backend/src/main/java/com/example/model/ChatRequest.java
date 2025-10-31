package com.example.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
public class ChatRequest {
    private String model = "qwen3:1.7b";
    private List<Map<String, String>> messages;
    private boolean stream = true;
    private List<Map<String, Object>> tools;
    private String tool_choice = "";
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Message {
        private String role;
        private String content;

        public static Map<String, String> system(String content) {
            return Map.of("role", "system", "content", content);
        }

        public static Map<String, String> user(String content) {
            return Map.of("role", "user", "content", content);
        }

        public static Map<String, String> assistant(String content) {
            return Map.of("role", "assistant", "content", content);
        }
    }
}
