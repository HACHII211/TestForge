package com.example.controller;

import com.example.common.Result;
import com.example.entity.ConversationInfo;
import com.example.service.ConversationInfoService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/conversation-names")
public class ConversationInfoController {

    @Resource
    private ConversationInfoService infoService;

    @GetMapping
    public Result listByUser(@RequestParam(required = false) Long userId) {
        if (userId == null) {
            return Result.error("400","userId is required");
        }
        List<ConversationInfo> list = infoService.selectByUserId(userId);
        return Result.success(list);
    }

    @GetMapping("/user/{userId}")
    public Result listByUserPath(@PathVariable Long userId) {
        List<ConversationInfo> list = infoService.selectByUserId(userId);
        return Result.success(list);
    }

    @GetMapping("/{id}")
    public Result getById(@PathVariable Long id) {
        ConversationInfo cn = infoService.selectById(id);
        return Result.success(cn);
    }

    @PostMapping
    public Result add(@RequestBody ConversationInfo cn) {
        infoService.addConversationName(cn);
        return Result.success(cn);
    }

    @PutMapping("/{id}")
    public Result update(@PathVariable Long id, @RequestBody ConversationInfo cn) {
        cn.setId(id);
        infoService.updateConversationName(cn);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Long id) {
        infoService.deleteConversationName(id);
        return Result.success();
    }
}
