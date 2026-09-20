package com.smartsell.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartsell.entity.ChatMessage;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
public class GeminiClientService {

    @Value("${app.ai.gemini-api-key}")
    private String geminiApiKey;

    private static final Logger log = LoggerFactory.getLogger(GeminiClientService.class);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    // Fallback models if API fetch fails (known good models)
    private static final List<String> FALLBACK_MODELS = List.of(
        "gemini-2.0-flash",
        "gemini-2.0-flash-lite",
        "gemini-1.5-flash",
        "gemini-1.5-flash-8b"
    );

    // Will be populated dynamically from Google API at startup
    private List<String> MODEL_CANDIDATES = new ArrayList<>(FALLBACK_MODELS);

    public GeminiClientService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Fetches available Gemini models from Google API at startup and caches them.
     * Filters to only flash/pro models that support generateContent.
     * Falls back to FALLBACK_MODELS if API call fails.
     */
    @PostConstruct
    public void fetchAvailableModels() {
        if (geminiApiKey == null || geminiApiKey.isBlank() || geminiApiKey.contains("YOUR_GEMINI_API_KEY")) {
            log.warn("[GeminiClientService] API key not configured — using fallback model list.");
            return;
        }
        try {
            String url = "https://generativelanguage.googleapis.com/v1beta/models?key=" + geminiApiKey;
            String responseStr = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(responseStr);
            JsonNode modelsNode = root.path("models");

            List<String> fetchedModels = StreamSupport.stream(modelsNode.spliterator(), false)
                .filter(m -> {
                    // Only include models that support generateContent
                    JsonNode methods = m.path("supportedGenerationMethods");
                    boolean supportsGenerate = StreamSupport.stream(methods.spliterator(), false)
                        .anyMatch(n -> "generateContent".equals(n.asText()));
                    // Prefer flash/pro models only
                    String name = m.path("name").asText("");
                    boolean isFlashOrPro = name.contains("flash") || name.contains("pro");
                    return supportsGenerate && isFlashOrPro;
                })
                .map(m -> {
                    // name is like "models/gemini-2.0-flash" — strip prefix
                    String fullName = m.path("name").asText("");
                    return fullName.startsWith("models/") ? fullName.substring(7) : fullName;
                })
                // Prioritize: newer flash first (2.x before 1.x), then pro, then rest
                .sorted((a, b) -> {
                    int scoreA = modelPriority(a);
                    int scoreB = modelPriority(b);
                    return Integer.compare(scoreB, scoreA); // descending
                })
                .collect(Collectors.toList());

            if (!fetchedModels.isEmpty()) {
                MODEL_CANDIDATES = fetchedModels;
                log.info("[GeminiClientService] Loaded {} models from API: {}", fetchedModels.size(), fetchedModels);
            } else {
                log.warn("[GeminiClientService] No models found from API — using fallback list.");
            }
        } catch (Exception e) {
            log.warn("[GeminiClientService] Failed to fetch models from API ({}), using fallback list.", e.getMessage());
        }
    }

    /** Priority score for sorting models (higher = tried first) */
    private int modelPriority(String name) {
        if (name.contains("2.0") && name.contains("flash") && !name.contains("lite")) return 100;
        if (name.contains("2.0") && name.contains("flash-lite")) return 90;
        if (name.contains("2.5") && name.contains("flash")) return 95;
        if (name.contains("1.5") && name.contains("flash") && !name.contains("8b")) return 80;
        if (name.contains("1.5") && name.contains("flash-8b")) return 70;
        if (name.contains("pro")) return 60;
        return 10;
    }

    private final String SHOPPING_ASSISTANT_PROMPT = """
      คุณคือ SmartSell AI ผู้ช่วยแนะนำการเลือกซื้อเสื้อผ้าส่วนบุคคลของร้านค้ามหาวิทยาลัย UTCC
      หน้าที่หลักของคุณคือ สอบถามความต้องการ (Preferences) ของลูกค้า เพื่อนำไปแนะนำสินค้าที่เหมาะสม
      
      ลำดับการถาม Preference มีเพียง 4 ด้านนี้เท่านั้น (ห้ามถามสีมงคล หรือ lucky_color โดยเด็ดขาด เพราะแยกฟีเจอร์ไปแล้ว):
      1. personal_color (Spring, Summer, Autumn, Winter)
      2. occasion (เช่น ทั่วไป, เข้ากิจกรรม, ทางการ)
      3. budget_min / budget_max (เช่น ต่ำกว่า 350, 300 - 500, มากกว่า 500)
      4. size (เช่น S, M, L, XL, XXL)
      
      กฎสำคัญมาก:
      - ต้องสื่อสารและถาม-ตอบเป็น "ภาษาไทย" เท่านั้น ด้วยน้ำเสียงสุภาพและเป็นกันเอง (ลงท้ายด้วย ค่ะ/นะคะ)
      - ห้ามถามเรื่องสีมงคล หรือ lucky color เป็นอันขาด!
      - ถามเพียง 1 ข้อต่อครั้งตามลำดับข้างต้น
      - อย่าสร้างคำถามนอกเหนือจาก 4 ข้อนี้
      - เมื่อได้ข้อมูลครบทั้ง 4 ด้านแล้ว ไม่ต้องถามคำถามเพิ่ม ให้แจ้งลูกค้าว่าได้ข้อมูลครบถ้วนแล้วและกำลังแนะนำสินค้าให้
      - อย่าแนะนำชื่อสินค้าหรือคำนวณคะแนนด้วยตนเอง (ระบบหลังบ้านจะแสดงสินค้าให้เอง)
      - ตอบสั้นกระชับ เป็นธรรมชาติ
      
      กรณีพิเศษ — Personal Color:
      - ถ้าลูกค้าบอกว่า "ไม่รู้" Personal Color: ให้ตอบรับทราบอย่างสุภาพ และคง personal_color เป็น null เพื่อรอให้ลูกค้าตัดสินใจก่อน
        ห้าม set personal_color เป็น Unknown โดยเด็ดขาด! ระบบจะแสดงตัวเลือกให้ลูกค้าเลือกเองค่ะ
      - ถ้าลูกค้าบอกว่าต้องการ "ข้าม" หรือ "ข้ามไปคำถามต่อไป" หรือ "skip" อย่างชัดเจน: จึงค่อย set personal_color = "Unknown"
        แล้วถามคำถามถัดไปทันที (occasion)
      
      คุณต้องตอบกลับเป็นรูปแบบ JSON เสมอ:
      {
        "reply": "ข้อความภาษาไทยที่คุยกับลูกค้า",
        "preferences": {
           "personal_color": "ค่าที่ระบุ หรือ Unknown หรือ null",
           "occasion": "ค่าที่ระบุ หรือ null",
           "budget_min": ตัวเลข หรือ null,
           "budget_max": ตัวเลข หรือ null,
           "size": "ไซส์ หรือ null"
        }
      }
      """;

    private final String PRODUCT_CHATBOT_PROMPT = """
      คุณคือ SmartSell AI ผู้เชี่ยวชาญบริการลูกค้าและตอบคำถามสินค้าของร้านค้ามหาวิทยาลัย UTCC (บริการแชท 24 ชม.)
      หน้าที่ของคุณคือ ตอบคำถามลูกค้าเกี่ยวกับสินค้าในร้านค้ามหาวิทยาลัย UTCC
      
      กฎเหล็กสำคัญที่สุด (ต้องปฏิบัติตามอย่างเคร่งครัด 100%):
      1. คุณต้องตอบคำถามโดยอ้างอิงจากข้อมูลใน "PRODUCT CONTEXT (ข้อมูลจากฐานข้อมูลของร้านค้า)" เท่านั้น
      2. หากลูกค้าถามคำถาม สินค้า เรื่อง หรือข้อมูลใดที่ไม่มีอยู่ใน PRODUCT CONTEXT หรือไม่มีในระบบฐานข้อมูล ให้ตอบอย่างสุภาพว่า:
         "ขออภัยค่ะ ไม่มีข้อมูลเรื่องนี้ในระบบฐานข้อมูลของร้านค้าค่ะ"
         ห้ามตอบสิ่งที่ไม่มีในฐานข้อมูล ห้ามแต่งเติม ห้ามเดาข้อมูลขึ้นมาเองโดยเด็ดขาด!
      3. ต้องตอบเป็น "ภาษาไทย" เท่านั้น ด้วยน้ำเสียงสุภาพ นอบน้อม และเป็นมิตร (ลงท้ายด้วย ค่ะ/นะคะ)
      4. ให้ตอบเป็นข้อความธรรมดา (ไม่ใช่ JSON) กระชับ ตรงประเด็น เข้าใจง่าย
      """;

    public AssistantResponse generateAssistantReply(List<ChatMessage> history, String newMessage) {
        String jsonResponseStr = callGeminiAPI(SHOPPING_ASSISTANT_PROMPT, history, newMessage, null);
        
        if (jsonResponseStr != null && !jsonResponseStr.isBlank()) {
            try {
                // Clean markdown code fence if present
                String cleaned = jsonResponseStr.trim();
                if (cleaned.startsWith("```json")) {
                    cleaned = cleaned.substring(7);
                    if (cleaned.endsWith("```")) cleaned = cleaned.substring(0, cleaned.length() - 3);
                } else if (cleaned.startsWith("```")) {
                    cleaned = cleaned.substring(3);
                    if (cleaned.endsWith("```")) cleaned = cleaned.substring(0, cleaned.length() - 3);
                }
                cleaned = cleaned.trim();
                
                JsonNode rootNode = objectMapper.readTree(cleaned);
                String reply = rootNode.has("reply") ? rootNode.get("reply").asText() : cleaned;
                JsonNode prefs = rootNode.has("preferences") ? rootNode.get("preferences") : null;
                
                AssistantResponse resp = new AssistantResponse();
                resp.setReply(reply);
                
                if (prefs != null) {
                    if (prefs.has("personal_color") && !prefs.get("personal_color").isNull()) resp.setPersonalColor(prefs.get("personal_color").asText());
                    if (prefs.has("occasion") && !prefs.get("occasion").isNull()) resp.setOccasion(prefs.get("occasion").asText());
                    if (prefs.has("size") && !prefs.get("size").isNull()) resp.setSize(prefs.get("size").asText());
                    if (prefs.has("budget_min") && !prefs.get("budget_min").isNull()) resp.setBudgetMin(prefs.get("budget_min").asDouble());
                    if (prefs.has("budget_max") && !prefs.get("budget_max").isNull()) resp.setBudgetMax(prefs.get("budget_max").asDouble());
                }
                
                // If reply is valid and in Thai
                if (resp.getReply() != null && !resp.getReply().isEmpty()) {
                    return resp;
                }
            } catch (Exception e) {
                // Fallback to rule-based parser
            }
        }
        
        // Smart rule-based extraction fallback (always in Thai and never asks lucky color)
        return handleFallback(newMessage);
    }

    private AssistantResponse handleFallback(String newMessage) {
        AssistantResponse resp = new AssistantResponse();
        String lower = newMessage.toLowerCase().trim();
        
        // 1. Personal Color
        if (lower.contains("spring") || lower.contains("ใบไม้ผลิ")) {
            resp.setPersonalColor("Spring");
            resp.setReply("รับทราบโทนสี Spring สดใสค่ะ! แล้วคุณอยากได้ชุดสำหรับใส่ในโอกาสไหนเป็นพิเศษคะ? (เช่น ทั่วไป, เข้ากิจกรรม, ทางการ)");
            return resp;
        } else if (lower.contains("summer") || lower.contains("ฤดูร้อน")) {
            resp.setPersonalColor("Summer");
            resp.setReply("รับทราบโทนสี Summer สบายตาค่ะ! แล้วคุณอยากได้ชุดสำหรับใส่ในโอกาสไหนเป็นพิเศษคะ? (เช่น ทั่วไป, เข้ากิจกรรม, ทางการ)");
            return resp;
        } else if (lower.contains("autumn") || lower.contains("ใบไม้ร่วง")) {
            resp.setPersonalColor("Autumn");
            resp.setReply("รับทราบโทนสี Autumn อบอุ่นคลาสสิกค่ะ! แล้วคุณอยากได้ชุดสำหรับใส่ในโอกาสไหนเป็นพิเศษคะ? (เช่น ทั่วไป, เข้ากิจกรรม, ทางการ)");
            return resp;
        } else if (lower.contains("winter") || lower.contains("หนาว")) {
            resp.setPersonalColor("Winter");
            resp.setReply("รับทราบโทนสี Winter คมชัดโดดเด่นค่ะ! แล้วคุณอยากได้ชุดสำหรับใส่ในโอกาสไหนเป็นพิเศษคะ? (เช่น ทั่วไป, เข้ากิจกรรม, ทางการ)");
            return resp;
        } else if (lower.contains("ไม่รู้")) {
            resp.setPersonalColor("Spring"); // Default friendly palette
            resp.setReply("ไม่เป็นไรเลยค่ะ! เราสามารถเริ่มต้นด้วยโทนสียอดนิยมได้ แล้วคุณอยากได้ชุดสำหรับใส่ในโอกาสไหนคะ? (เช่น ทั่วไป, เข้ากิจกรรม, ทางการ)");
            return resp;
        }
        
        // 2. Occasion
        if (lower.contains("กิจกรรม") || lower.contains("ทั่วไป") || lower.contains("ทางการ") || lower.contains("เรียน") || lower.contains("เที่ยว") || lower.contains("ทำงาน") || lower.contains("casual") || lower.contains("formal")) {
            resp.setOccasion(newMessage.trim());
            resp.setReply("บันทึกโอกาสการใช้งานเรียบร้อยค่ะ! คุณมีงบประมาณสำหรับการแต่งตัวครั้งนี้เท่าไหร่คะ? (เช่น ต่ำกว่า 350, 300 - 500, มากกว่า 500)");
            return resp;
        }

        // 3. Budget
        if (lower.contains("350") && (lower.contains("ต่ำกว่า") || lower.contains("<"))) {
            resp.setBudgetMin(0.0);
            resp.setBudgetMax(350.0);
            resp.setReply("บันทึกงบประมาณต่ำกว่า 350 บาทเรียบร้อยค่ะ! คุณสวมเสื้อผ้าไซส์ไหนเป็นประจำคะ? (S, M, L, XL, XXL)");
            return resp;
        } else if (lower.contains("300") && lower.contains("500")) {
            resp.setBudgetMin(300.0);
            resp.setBudgetMax(500.0);
            resp.setReply("บันทึกงบประมาณช่วง 300 - 500 บาทเรียบร้อยค่ะ! คุณสวมเสื้อผ้าไซส์ไหนเป็นประจำคะ? (S, M, L, XL, XXL)");
            return resp;
        } else if (lower.contains("500") && (lower.contains("มากกว่า") || lower.contains(">"))) {
            resp.setBudgetMin(500.0);
            resp.setBudgetMax(2000.0);
            resp.setReply("บันทึกงบประมาณมากกว่า 500 บาทเรียบร้อยค่ะ! คุณสวมเสื้อผ้าไซส์ไหนเป็นประจำคะ? (S, M, L, XL, XXL)");
            return resp;
        }
        
        // 4. Size (Final step - no lucky color question!)
        if (lower.equals("s") || lower.equals("m") || lower.equals("l") || lower.equals("xl") || lower.equals("xxl")) {
            String size = lower.toUpperCase();
            resp.setSize(size);
            resp.setReply("บันทึกไซส์ " + size + " ให้เรียบร้อยค่ะ! ได้ข้อมูลครบถ้วนแล้ว ระบบได้คัดเลือกสินค้าที่เหมาะกับคุณที่สุดมาให้ชมด้านล่างนะคะ ✨");
            return resp;
        }
        
        // Default welcome in Thai
        resp.setReply("ยินดีต้อนรับสู่ SmartSell AI ค่ะ! เพื่อช่วยแนะนำชุดที่เหมาะกับคุณที่สุด ขอเริ่มถาม Personal Color ก่อนนะคะ — โทนสีของคุณเป็นแบบไหน? (Spring, Summer, Autumn, Winter)");
        return resp;
    }

    public String generateSupportReply(List<ChatMessage> history, String newMessage, String productContext) {
        String reply = callGeminiAPI(PRODUCT_CHATBOT_PROMPT, history, newMessage, productContext);
        if (reply != null && !reply.isBlank()) {
            return reply;
        }
        return "ขออภัยค่ะ ไม่มีข้อมูลเรื่องนี้ในระบบฐานข้อมูลของร้านค้าค่ะ";
    }

    private String callGeminiAPI(String systemPrompt, List<ChatMessage> history, String newMessage, String context) {
        if (geminiApiKey == null || geminiApiKey.isBlank() || geminiApiKey.contains("YOUR_GEMINI_API_KEY")) {
            return null;
        }

        for (String modelName : MODEL_CANDIDATES) {
            try {
                Map<String, Object> requestBody = new HashMap<>();
                
                // System instruction
                Map<String, Object> sysInstr = new HashMap<>();
                sysInstr.put("parts", List.of(Map.of("text", systemPrompt)));
                requestBody.put("systemInstruction", sysInstr);
                
                // Contents (History + New Message)
                List<Map<String, Object>> contents = new ArrayList<>();
                if (history != null) {
                    for (ChatMessage msg : history) {
                        contents.add(Map.of(
                            "role", "USER".equalsIgnoreCase(msg.getSender()) ? "user" : "model",
                            "parts", List.of(Map.of("text", msg.getContent()))
                        ));
                    }
                }
                
                String finalMessage = newMessage;
                if (context != null) {
                    finalMessage = "PRODUCT CONTEXT:\n" + context + "\n\nUSER MESSAGE:\n" + newMessage;
                }
                
                contents.add(Map.of(
                    "role", "user",
                    "parts", List.of(Map.of("text", finalMessage))
                ));
                
                requestBody.put("contents", contents);
                
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
                
                String url = "https://generativelanguage.googleapis.com/v1beta/models/" + modelName + ":generateContent?key=" + geminiApiKey;
                String responseStr = restTemplate.postForObject(url, entity, String.class);
                
                // Parse response safely
                JsonNode rootNode = objectMapper.readTree(responseStr);
                JsonNode candidates = rootNode.path("candidates");
                if (candidates.isArray() && candidates.size() > 0) {
                    JsonNode firstCandidate = candidates.get(0);
                    // Check finish reason - skip if SAFETY or RECITATION
                    String finishReason = firstCandidate.path("finishReason").asText("");
                    if ("SAFETY".equals(finishReason) || "RECITATION".equals(finishReason)) {
                        continue; // try next model
                    }
                    JsonNode textNode = firstCandidate.path("content").path("parts");
                    if (textNode.isArray() && textNode.size() > 0) {
                        String text = textNode.get(0).path("text").asText("");
                        if (!text.isBlank()) {
                            return text;
                        }
                    }
                }
                // If we reach here, response was empty — try next model
                
            } catch (Exception e) {
                // Continue to next candidate model
            }
        }
        return null;
    }

    public static class AssistantResponse {
        private String reply;
        private String personalColor;
        private String occasion;
        private Double budgetMin;
        private Double budgetMax;
        private String size;

        public String getReply() { return reply; }
        public void setReply(String reply) { this.reply = reply; }
        public String getPersonalColor() { return personalColor; }
        public void setPersonalColor(String personalColor) { this.personalColor = personalColor; }
        public String getOccasion() { return occasion; }
        public void setOccasion(String occasion) { this.occasion = occasion; }
        public Double getBudgetMin() { return budgetMin; }
        public void setBudgetMin(Double budgetMin) { this.budgetMin = budgetMin; }
        public Double getBudgetMax() { return budgetMax; }
        public void setBudgetMax(Double budgetMax) { this.budgetMax = budgetMax; }
        public String getSize() { return size; }
        public void setSize(String size) { this.size = size; }
    }
}
