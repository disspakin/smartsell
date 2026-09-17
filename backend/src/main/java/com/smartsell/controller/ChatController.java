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

    // POST /api/chat/assistant  — used by the Personal Shopping Assistant
    @PostMapping("/assistant")
    public ChatDTO.ChatResponse chatAssistant(@RequestBody ChatDTO.ChatRequest request) {
        return chatService.handleShoppingAssistantChat(request);
    }

    // POST /api/chat/support  — used by the AI Chat 24hrs.
    @PostMapping("/support")
    public ChatDTO.ChatResponse chatSupport(@RequestBody ChatDTO.ChatRequest request) {
        return chatService.handleGeneralSupportChat(request);
    }
}
