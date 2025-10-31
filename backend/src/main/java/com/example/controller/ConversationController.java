package com.example.controller;

import com.example.common.Result;
import com.example.entity.Conversation;
import com.example.service.ConversationService;
import com.github.pagehelper.PageInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/conversations")
public class ConversationController {

    @Resource
    private ConversationService conversationService;

    private final ObjectMapper MAPPER = new ObjectMapper();

    @GetMapping
    public Result list(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String assistantId,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize
    ) {
        Map<String, Object> params = new HashMap<>();
        if (userId != null) params.put("userId", userId);
        if (assistantId != null && !assistantId.isEmpty()) params.put("assistantId", assistantId);
        if (dateFrom != null && !dateFrom.isEmpty()) params.put("dateFrom", dateFrom);
        if (dateTo != null && !dateTo.isEmpty()) params.put("dateTo", dateTo);
        if (q != null && !q.isEmpty()) params.put("q", q);

        PageInfo<Conversation> page = conversationService.selectByPage(params, pageNum, pageSize);
        return Result.success(page);
    }

    @GetMapping("/{id}")
    public Result getById(@PathVariable Long id) {
        Conversation conv = conversationService.selectById(id);
        return Result.success(conv);
    }

    @GetMapping("/by-uuid/{uuid}")
    public Result getByUuid(@PathVariable String uuid) {
        Conversation conv = conversationService.selectByUuid(uuid);
        return Result.success(conv);
    }

    @PostMapping
    public Result add(@RequestBody Conversation conv) {
        System.out.println(conv.getUserId()+" "+conv.getAssistantId());

        conversationService.addConversation(conv);

        return Result.success(conv);
    }

    @PutMapping("/{id}")
    public Result update(@PathVariable Long id, @RequestBody Conversation conv) {
        conv.setId(id);
        conversationService.updateConversation(conv);
        return Result.success();
    }


    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Long id) {
        conversationService.deleteConversation(id);
        return Result.success();
    }

    @DeleteMapping("/deleteBatch")
    public Result deleteBatch(@RequestBody List<Long> ids) {
        conversationService.deleteBatch(ids);
        return Result.success();
    }


    @PostMapping("/{uuid}/append")
    public Result appendMessage(@PathVariable String uuid, @RequestBody Map<String, Object> message) {
        try {
            String messageJson = MAPPER.writeValueAsString(message);
            // try to use timestamp if provided
            Object tsObj = message.get("timestamp");
            String ts = tsObj != null ? String.valueOf(tsObj) : null;
            conversationService.appendMessageByUuid(uuid, messageJson, ts);
            return Result.success();
        } catch (Exception ex) {
            ex.printStackTrace();
            return Result.error("400","invalid message payload");
        }
    }
}
