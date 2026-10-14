package com.smartsell.service;

import com.smartsell.dto.ChatDTO;
import com.smartsell.dto.ProductDTO;
import com.smartsell.entity.ChatMessage;
import com.smartsell.entity.CustomerSession;
import com.smartsell.entity.TargetGroup;
import com.smartsell.repository.ChatMessageRepository;
import com.smartsell.repository.CustomerSessionRepository;
import com.smartsell.repository.CustomerInteractionRepository;
import com.smartsell.repository.SizeChartRowRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ChatService {

    private final CustomerSessionRepository sessionRepository;
    private final ChatMessageRepository messageRepository;
    private final ProductScoringService productScoringService;
    private final GeminiClientService geminiClientService;
    private final com.smartsell.repository.ProductRepository productRepository;
    private final CustomerInteractionRepository customerInteractionRepository;
    private final StoreInfoService storeInfoService;
    private final SizeChartRowRepository sizeChartRowRepository;

    public ChatService(CustomerSessionRepository sessionRepository,
                       ChatMessageRepository messageRepository,
                       ProductScoringService productScoringService,
                       GeminiClientService geminiClientService,
                       com.smartsell.repository.ProductRepository productRepository,
                       CustomerInteractionRepository customerInteractionRepository,
                       StoreInfoService storeInfoService,
                       SizeChartRowRepository sizeChartRowRepository) {
        this.sessionRepository = sessionRepository;
        this.messageRepository = messageRepository;
        this.productScoringService = productScoringService;
        this.geminiClientService = geminiClientService;
        this.productRepository = productRepository;
        this.customerInteractionRepository = customerInteractionRepository;
        this.storeInfoService = storeInfoService;
        this.sizeChartRowRepository = sizeChartRowRepository;
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
        // ข้อมูลร้าน + เวลาไทยตอนนี้ + ตารางไซส์ ดึงใหม่ทุกข้อความ (ร้านแก้ใน DB แล้วมีผลทันที และสถานะเปิด/ปิดตรงเวลาจริง)
        GeminiClientService.AssistantResponse aiResult = geminiClientService.generateAssistantReply(history, request.message(), buildStoreContext());
        
        // Update session with extracted preferences
        if (aiResult.getPersonalColor() != null) session.setPersonalColor(aiResult.getPersonalColor());
        if (aiResult.getOccasion() != null) session.setOccasion(aiResult.getOccasion());
        if (aiResult.getSize() != null) session.setSize(aiResult.getSize());
        if (aiResult.getBudgetMin() != null) session.setBudgetMin(java.math.BigDecimal.valueOf(aiResult.getBudgetMin()));
        if (aiResult.getBudgetMax() != null) session.setBudgetMax(java.math.BigDecimal.valueOf(aiResult.getBudgetMax()));
        String gender = TargetGroup.normalizeCustomerGender(aiResult.getGender());
        if (gender != null) session.setGender(gender);
        session = sessionRepository.save(session);
        
        String aiReply = aiResult.getReply();
        saveMessage(session, "AI", aiReply);

        ChatDTO.PreferenceSummary summary = new ChatDTO.PreferenceSummary(
                session.getPersonalColor(), session.getOccasion(),
                formatBudget(session), session.getSize(), session.getGender()
        );

        // 4. Rulebase scoring — return top 3 products ONLY when all 5 preferences are collected
        // personal_color can be "Unknown" if user skipped, gender can be "ANY" (ไม่ระบุ) — both still count as complete
        boolean isPreferenceComplete = session.getPersonalColor() != null
                && session.getOccasion() != null
                && (session.getBudgetMin() != null || session.getBudgetMax() != null)
                && session.getSize() != null
                && session.getGender() != null;

        List<ProductDTO> products;
        if (isPreferenceComplete) {
            products = productScoringService.recommendByPersonalColor(
                    session.getPersonalColor(),
                    session.getOccasion(),
                    session.getBudgetMin(),
                    session.getBudgetMax(),
                    session.getSize(),
                    session.getGender(),
                    3
            );
            // Save tracking for AI_RECOMMENDED — นับครั้งเดียวต่อ session ต่อสินค้า
            // (ไม่งั้นทุกข้อความหลังเก็บ preference ครบจะบันทึกซ้ำอีก 3 แถว)
            for (ProductDTO pDto : products) {
                if (customerInteractionRepository.existsBySessionIdAndProductIdAndEventType(
                        session.getId(), pDto.id(), "AI_RECOMMENDED")) {
                    continue;
                }
                com.smartsell.entity.Product product = productRepository.findById(pDto.id()).orElse(null);
                if (product != null) {
                    com.smartsell.entity.CustomerInteraction interaction = new com.smartsell.entity.CustomerInteraction(
                            session, product, "AI_RECOMMENDED",
                            session.getPersonalColor(), session.getOccasion(),
                            null, session.getSize(), session.getBudgetMax()
                    );
                    customerInteractionRepository.save(interaction);
                }
            }
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
            contextBuilder.append("- สำหรับ: ").append(TargetGroup.thaiLabel(p.getTargetGroup())).append("\n");
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
        
        String aiReply = geminiClientService.generateSupportReply(history, request.message(), contextBuilder.toString(), buildStoreContext());
        saveMessage(session, "AI", aiReply);

        return new ChatDTO.ChatResponse(session.getId(), aiReply, null, List.of());
    }

    /** ข้อมูลร้าน + เวลาไทยตอนนี้ + ตารางไซส์ — ดึงใหม่ทุกข้อความ ใช้ทั้งผู้ช่วย AI และแชทกับเรา */
    private String buildStoreContext() {
        return storeInfoService.buildChatContext(storeInfoService.now()) + "\n" + buildSizeChartContext();
    }

    /**
     * ตารางไซส์ของทุก chart ที่มีสินค้าผูกอยู่ (หน่วยนิ้ว วัดตัวเสื้อ) พร้อมชื่อสินค้าที่ใช้ตารางนั้น
     * ให้ AI ตอบเรื่องไซส์ได้ — chart ที่ is_estimated = true ติดป้ายค่าประมาณไว้ให้ AI บอกลูกค้า
     */
    private String buildSizeChartContext() {
        Map<com.smartsell.entity.SizeChart, List<String>> productsByChart = new LinkedHashMap<>();
        for (com.smartsell.entity.Product p : productRepository.findAll()) {
            if (p.getSizeChart() != null) {
                productsByChart.computeIfAbsent(p.getSizeChart(), c -> new java.util.ArrayList<>()).add(p.getName());
            }
        }
        StringBuilder sb = new StringBuilder("ตารางไซส์ของสินค้า (หน่วยนิ้ว เป็นขนาดตัวเสื้อ รูปแบบ ไซส์ รอบอก/ความยาว):\n");
        if (productsByChart.isEmpty()) {
            sb.append("ยังไม่มีตารางไซส์\n");
        }
        productsByChart.forEach((chart, productNames) -> {
            sb.append(chart.getName());
            if (chart.isEstimated()) sb.append(" [ค่าประมาณ ยังไม่ยืนยันจากร้าน]");
            sb.append(" — ใช้กับ: ").append(String.join(", ", productNames)).append("\n  ");
            sb.append(sizeChartRowRepository.findBySizeChartIdOrderBySortOrder(chart.getId()).stream()
                    .map(r -> r.getSize() + " " + r.getChestIn().stripTrailingZeros().toPlainString()
                            + "/" + r.getLengthIn().stripTrailingZeros().toPlainString())
                    .collect(Collectors.joining(", ")));
            sb.append("\n");
        });
        return sb.toString();
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