package com.example.controller;

import com.example.service.QAService;
import com.example.common.Result;
import com.example.entity.QA;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/qa")
public class QAController {

    @Resource
    private QAService qaService;

    // SSE流式问答接口
    @PostMapping(value = "/askStream", consumes = "application/json", produces = "text/event-stream")
    public SseEmitter askStream(@RequestBody List<Map<String, String>> messages) {
        if (messages == null || messages.isEmpty()) {
            throw new IllegalArgumentException("Messages cannot be null or empty");
        }

        SseEmitter emitter = new SseEmitter(0L); // 永不超时
        new Thread(() -> {
            try {
                qaService.askStream(messages, emitter);
                emitter.complete();
            } catch (Exception ex) {
                emitter.completeWithError(ex);
            }
        }).start();

        return emitter;
    }

    // 问答 + 执行 SQL 查询接口
    @PostMapping("/ask/sql")
    public Result askAndQuery(@RequestBody QA qa) {
        String question = qa.getQuestion();
        boolean txt2sql = qa.isTxt2sql();

        if (!txt2sql || question == null || question.trim().isEmpty()) {
            return Result.error();
        }

        try {
            String result = qaService.executeGeneratedSql(question);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error();
        }
    }

    // 生成 SQL 语句接口
    @PostMapping("/genSql")
    public Result generateSql(@RequestBody QA qa) {
        String question = qa.getQuestion();
        if (question == null || question.trim().isEmpty()) {
            return Result.error();
        }

        try {
            String sql = qaService.generateSql(question);
            return Result.success(sql);
        } catch (Exception e) {
            return Result.error();
        }
    }

    @PostMapping(value = "/normalAsk", consumes = "application/json", produces = "text/event-stream")
    public SseEmitter normalAsk(@RequestBody String question, String require) {
        List<Map<String, String>> test_messages = Arrays.asList(
                new HashMap<String, String>() {{
                    put("role", "system");
                    put("content", require);
                }},
                new HashMap<String, String>() {{
                    put("role", "user");
                    put("content", question);
                }}
        );
        SseEmitter emitter = new SseEmitter(0L); // 永不超时
        new Thread(() -> {
            try {
                qaService.normalAsk(test_messages, emitter);
                emitter.complete();
            } catch (Exception ex) {
                emitter.completeWithError(ex);
            }
        }).start();

        return emitter;
    }
}
