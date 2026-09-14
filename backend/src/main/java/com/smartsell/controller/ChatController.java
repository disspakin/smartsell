package com.smartsell.controller;

import com.smartsell.dto.ChatDTO;
import com.smartsell.service.ChatService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    // POST /api/chat  — used by the Personal Shopping Assistant page's input bar
    @PostMapping
    public ChatDTO.ChatResponse chat(@RequestBody ChatDTO.ChatRequest request) {
        return chatService.handleMessage(request);
    }
}
