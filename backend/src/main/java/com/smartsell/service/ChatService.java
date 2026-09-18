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
    private final GeminiService geminiService;

    public ChatService(CustomerSessionRepository sessionRepository,
                       ChatMessageRepository messageRepository,
                       ProductScoringService productScoringService,
                       GeminiService geminiService) {
        this.sessionRepository = sessionRepository;
        this.messageRepository = messageRepository;
        this.productScoringService = productScoringService;
        this.geminiService = geminiService;
    }

    // =====================================================================
    // 1. Personal Shopping Assistant
    // =====================================================================
    public ChatDTO.ChatResponse handleShoppingAssistantChat(ChatDTO.ChatRequest request) {
        CustomerSession session = (request.sessionId() != null)
                ? sessionRepository.findById(request.sessionId()).orElseGet(this::newSession)
                : newSession();
        session = sessionRepository.save(session);

        saveMessage(session, "USER", request.message());

        String systemInstruction = """
                คุณคือผู้ช่วยขายเสื้อผ้าออนไลน์ของร้าน SmartSell AI
                หน้าที่ของคุณคือช่วยแนะนำเสื้อผ้าที่เหมาะกับลูกค้า โดยถามข้อมูลเพิ่มเติมถ้าจำเป็น เช่น
                โทนสีผิว (personal color), โอกาสที่จะใส่, งบประมาณ, และไซส์ที่ต้องการ
                ตอบด้วยน้ำเสียงสุภาพ เป็นกันเอง กระชับ ไม่ยาวเกินไป ใช้ภาษาไทย
                """;
        String aiReply = geminiService.ask(systemInstruction, request.message());
        saveMessage(session, "AI", aiReply);

        ChatDTO.PreferenceSummary summary = new ChatDTO.PreferenceSummary(
                session.getPersonalColor(), session.getOccasion(),
                formatBudget(session), session.getSize()
        );

        // แก้ NPE: เรียกหาสินค้าแนะนำ ก็ต่อเมื่อรู้ personal color ของลูกค้าแล้วเท่านั้น
        // (ถ้ายังไม่รู้ personal color เช่นเพิ่งเริ่มแชท ให้ส่ง list ว่างไปก่อน ไม่ให้ทั้ง request พัง)
        List<ProductDTO> products;
        if (session.getPersonalColor() != null) {
            products = productScoringService.recommendByPersonalColor(
                    session.getPersonalColor(),
                    session.getOccasion(),
                    3
            );
        } else {
            products = List.of();
        }

        return new ChatDTO.ChatResponse(session.getId(), aiReply, summary, products);
    }

    // =====================================================================
    // 2. AI Chat 24hrs. (General Customer Service)
    // =====================================================================
    public ChatDTO.ChatResponse handleGeneralSupportChat(ChatDTO.ChatRequest request) {
        CustomerSession session = (request.sessionId() != null)
                ? sessionRepository.findById(request.sessionId()).orElseGet(this::newSession)
                : newSession();
        session = sessionRepository.save(session);

        saveMessage(session, "USER", request.message());

        String systemInstruction = """
                คุณคือแอดมินฝ่ายบริการลูกค้าของร้าน SmartSell AI (ร้านขายเสื้อผ้าออนไลน์)
                ตอบคำถามลูกค้าเกี่ยวกับสินค้า การจัดส่ง การคืนสินค้า หรือคำถามทั่วไป
                ด้วยน้ำเสียงสุภาพ กระชับ เป็นภาษาไทย ถ้าไม่แน่ใจข้อมูล ให้บอกตรง ๆ ว่าต้องเช็คให้ก่อน
                """;
        String aiReply = geminiService.ask(systemInstruction, request.message());
        saveMessage(session, "AI", aiReply);

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