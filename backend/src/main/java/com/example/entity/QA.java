// com/example/entity/QA.java
package com.example.entity;

import java.util.List;
import java.util.Map;

public class QA {
    // 如果 txt2sql=true，则前端也会单独调用 /genSql 和 /ask/sql，这里保留字段
    private boolean txt2sql;
    // 兼容老接口：单问句
    private String question;
    // 新增：完整的 chat messages 列表
    private List<Map<String, Object>> messages;

    // --- getters & setters ---
    public boolean isTxt2sql() { return txt2sql; }
    public void setTxt2sql(boolean txt2sql) { this.txt2sql = txt2sql; }

    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }

    public List<Map<String, Object>> getMessages() { return messages; }
    public void setMessages(List<Map<String, Object>> messages) { this.messages = messages; }
}
