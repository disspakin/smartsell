package com.smartsell.service;

import com.smartsell.dto.ChatDTO;
import com.smartsell.entity.ChatMessage;
import com.smartsell.entity.CustomerSession;
import com.smartsell.repository.ChatMessageRepository;
import com.smartsell.repository.CustomerSessionRepository;
import org.springframework.stereotype.Service;

@Service
public class ChatService {

    private final CustomerSessionRepository sessionRepository;
    private final ChatMessageRepository messageRepository;

    public ChatService(CustomerSessionRepository sessionRepository, ChatMessageRepository messageRepository) {
        this.sessionRepository = sessionRepository;
        this.messageRepository = messageRepository;
    }

    public ChatDTO.ChatResponse handleMessage(ChatDTO.ChatRequest request) {
        // 1. get or create the session
        CustomerSession session = (request.sessionId() != null)
                ? sessionRepository.findById(request.sessionId()).orElseGet(this::newSession)
                : newSession();
        session = sessionRepository.save(session);

        // 2. save the user's message
        saveMessage(session, "USER", request.message());

        // 3. TODO: call Gemini/OpenAI here, passing the conversation so far,
        //    and ask it to (a) reply naturally, and (b) extract any of
        //    personal_color / occasion / budget / size it can find in the reply,
        //    then write those fields onto `session` and save().
        //    This stub just echoes back so the endpoint is runnable end-to-end.
        String aiReply = "（ตัวอย่างคำตอบ — ต่อ Gemini/OpenAI API ตรงนี้） รับทราบค่ะ: " + request.message();
        saveMessage(session, "AI", aiReply);

        ChatDTO.PreferenceSummary summary = new ChatDTO.PreferenceSummary(
                session.getPersonalColor(), session.getOccasion(),
                formatBudget(session), session.getSize()
        );

        return new ChatDTO.ChatResponse(session.getId(), aiReply, summary);
    }

    private CustomerSession newSession() {
        return new CustomerSession();
    }

    private void saveMessage(CustomerSession session, String sender, String content) {
        ChatMessage msg = new ChatMessage();
        msg.setSession(session);
        msg.setSender(sender);
        msg.setContent(content);
        messageRepository.save(msg);
    }

    private String formatBudget(CustomerSession s) {
        if (s.getBudgetMin() == null && s.getBudgetMax() == null) return null;
        return s.getBudgetMin() + " - " + s.getBudgetMax();
    }
}
