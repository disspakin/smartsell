package com.smartsell.service;

import com.smartsell.dto.ChatDTO;
import com.smartsell.dto.ProductDTO;
import com.smartsell.entity.ChatMessage;
import com.smartsell.entity.CustomerSession;
import com.smartsell.repository.ChatMessageRepository;
import com.smartsell.repository.CustomerSessionRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ChatService {

    private final CustomerSessionRepository sessionRepository;
    private final ChatMessageRepository messageRepository;
    private final ProductScoringService productScoringService;

    public ChatService(CustomerSessionRepository sessionRepository,
                       ChatMessageRepository messageRepository,
                       ProductScoringService productScoringService) {
        this.sessionRepository = sessionRepository;
        this.messageRepository = messageRepository;
        this.productScoringService = productScoringService;
    }

    // =====================================================================
    // 1. Personal Shopping Assistant
    // =====================================================================
    public ChatDTO.ChatResponse handleShoppingAssistantChat(ChatDTO.ChatRequest request) {
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
        String aiReply = "（ตัวอย่างคำตอบ Assistant — ต่อ Gemini/OpenAI API ตรงนี้） รับทราบค่ะ กำลังหาชุดที่เหมาะกับคุณนะคะ: " + request.message();
        saveMessage(session, "AI", aiReply);

        ChatDTO.PreferenceSummary summary = new ChatDTO.PreferenceSummary(
                session.getPersonalColor(), session.getOccasion(),
                formatBudget(session), session.getSize()
        );

        // 4. Rulebase scoring — return top 3 products based on session preferences
        List<ProductDTO> products = productScoringService.recommendByPersonalColor(
                session.getPersonalColor(),
                session.getOccasion(),
                3
        );

        return new ChatDTO.ChatResponse(session.getId(), aiReply, summary, products);
    }

    // =====================================================================
    // 2. AI Chat 24hrs. (General Customer Service)
    // =====================================================================
    public ChatDTO.ChatResponse handleGeneralSupportChat(ChatDTO.ChatRequest request) {
        // 1. get or create the session
        CustomerSession session = (request.sessionId() != null)
                ? sessionRepository.findById(request.sessionId()).orElseGet(this::newSession)
                : newSession();
        session = sessionRepository.save(session);

        // 2. save the user's message
        saveMessage(session, "USER", request.message());

        // 3. TODO: Query Product/Variant Info from DB based on keyword extraction,
        //    then pass that context to LLM to answer the user's question accurately (Zero Hallucination).
        String aiReply = "（ตัวอย่างคำตอบ Support 24ชม. — ต่อ DB Retrieval + Gemini ตรงนี้） ข้อมูลเกี่ยวกับสินค้าที่คุณถาม: " + request.message();
        saveMessage(session, "AI", aiReply);

        // General support does not necessarily return recommended product cards, just the reply.
        return new ChatDTO.ChatResponse(session.getId(), aiReply, null, List.of());
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
