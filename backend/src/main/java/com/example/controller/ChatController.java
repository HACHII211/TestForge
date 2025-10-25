package com.example.controller;
import com.example.model.ChatRequest;
import com.example.service.ChatService;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@CrossOrigin
public class ChatController {

    @Resource
    private final ChatService chatService;

    @PostMapping("/normal")
    public SseEmitter chatStream(@RequestBody ChatRequest request) {
        SseEmitter emitter = new SseEmitter(2 * 60 * 1000L);

        // 在新线程中处理请求，避免阻塞
        new Thread(() -> {
            try {
                chatService.chat(request, emitter);
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        }).start();

        return emitter;
    }

}