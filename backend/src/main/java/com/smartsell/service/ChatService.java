package com.smartsell.service;

import com.smartsell.dto.ChatDTO;
import com.smartsell.dto.ProductDTO;
import com.smartsell.entity.ChatMessage;
import com.smartsell.entity.CustomerSession;
import com.smartsell.repository.ChatMessageRepository;
import com.smartsell.repository.CustomerSessionRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class ChatService {

    private final CustomerSessionRepository sessionRepository;
    private final ChatMessageRepository messageRepository;
    private final ProductScoringService productScoringService;
    private final GeminiClientService geminiClientService;
    private final com.smartsell.repository.ProductRepository productRepository;

    public ChatService(CustomerSessionRepository sessionRepository,
                       ChatMessageRepository messageRepository,
                       ProductScoringService productScoringService,
                       GeminiClientService geminiClientService,
                       com.smartsell.repository.ProductRepository productRepository) {
        this.sessionRepository = sessionRepository;
        this.messageRepository = messageRepository;
        this.productScoringService = productScoringService;
        this.geminiClientService = geminiClientService;
        this.productRepository = productRepository;
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

        // ── Parse user message → update session preferences ──────────────────
        // (Delegated to Gemini JSON parser)
        List<ChatMessage> history = messageRepository.findBySessionIdOrderByCreatedAtAsc(session.getId());
        GeminiClientService.AssistantResponse aiResult = geminiClientService.generateAssistantReply(history, request.message());
        
        // Update session with extracted preferences
        if (aiResult.getPersonalColor() != null) session.setPersonalColor(aiResult.getPersonalColor());
        if (aiResult.getOccasion() != null) session.setOccasion(aiResult.getOccasion());
        if (aiResult.getSize() != null) session.setSize(aiResult.getSize());
        if (aiResult.getBudgetMin() != null) session.setBudgetMin(java.math.BigDecimal.valueOf(aiResult.getBudgetMin()));
        if (aiResult.getBudgetMax() != null) session.setBudgetMax(java.math.BigDecimal.valueOf(aiResult.getBudgetMax()));
        session = sessionRepository.save(session);
        
        String aiReply = aiResult.getReply();
        saveMessage(session, "AI", aiReply);

        ChatDTO.PreferenceSummary summary = new ChatDTO.PreferenceSummary(
                session.getPersonalColor(), session.getOccasion(),
                formatBudget(session), session.getSize()
        );

        // 4. Rulebase scoring — return top 3 products ONLY when all 4 preferences are collected
        // personal_color can be "Unknown" if user skipped (still counts as complete)
        boolean isPreferenceComplete = session.getPersonalColor() != null
                && session.getOccasion() != null
                && (session.getBudgetMin() != null || session.getBudgetMax() != null)
                && session.getSize() != null;

        List<ProductDTO> products;
        if (isPreferenceComplete) {
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

        List<ChatMessage> history = messageRepository.findBySessionIdOrderByCreatedAtAsc(session.getId());
        
        // Fetch all products from DB for context
        List<com.smartsell.entity.Product> allProducts = productRepository.findAllWithVariants();
        StringBuilder contextBuilder = new StringBuilder();
        for (com.smartsell.entity.Product p : allProducts) {
            contextBuilder.append("สินค้า: ").append(p.getName()).append("\n");
            contextBuilder.append("- หมวดหมู่: ").append(p.getCategory()).append("\n");
            contextBuilder.append("- ราคา: ").append(p.getPrice()).append(" บาท\n");
            contextBuilder.append("- รายละเอียด: ").append(p.getDescription()).append("\n");
            if (p.getVariants() != null && !p.getVariants().isEmpty()) {
                contextBuilder.append("- ตัวเลือกที่มี (สี/ไซส์):\n");
                for (com.smartsell.entity.ProductVariant v : p.getVariants()) {
                    contextBuilder.append("  * สี ").append(v.getColor())
                                  .append(" ไซส์ ").append(v.getSize()).append("\n");
                }
            }
            contextBuilder.append("\n");
        }
        
        String aiReply = geminiClientService.generateSupportReply(history, request.message(), contextBuilder.toString());
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