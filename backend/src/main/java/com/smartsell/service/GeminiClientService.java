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
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
public class GeminiClientService {

    @Value("${app.ai.gemini-api-key}")
    private String geminiApiKey;

    private static final Logger log = LoggerFactory.getLogger(GeminiClientService.class);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    private static final String API_BASE = "https://generativelanguage.googleapis.com/v1beta/models";

    /**
     * ใช้เมื่อดึงรายชื่อ model จาก API ไม่ได้ — ต้องเป็น model ที่ยังเปิดให้ใช้จริง
     * (ทดสอบแล้ว 2026-10-05: รุ่น 2.0/2.5 ถูกปิดสำหรับบัญชีใหม่ ตอบ 404 ทุกคำขอ)
     */
    private static final List<String> FALLBACK_MODELS = List.of(
        "gemini-3.8-flash",
        "gemini-3-flash-preview",
        "gemini-3.1-flash-lite"
    );

    /** model ที่ไม่ได้ทำมาเพื่อแชทข้อความ — มี generateContent ก็จริง แต่ตอบเป็นเสียง/รูป หรือใช้งานเฉพาะทาง */
    private static final List<String> NON_CHAT_MODEL_KEYWORDS = List.of(
        "tts", "image", "audio", "live", "embedding", "computer-use", "robotics", "customtools", "thinking-exp"
    );

    private static final Pattern MODEL_NAME = Pattern.compile("^gemini-(\\d+(?:\\.\\d+)?)-(flash|pro)(-lite)?(.*)$");

    // ดึงจาก Google API ตอนเริ่มระบบ และตัด model ที่ตอบ 404 ทิ้งระหว่างใช้งาน
    // หลาย request ใช้ list นี้พร้อมกัน จึงใช้ CopyOnWriteArrayList
    private volatile List<String> MODEL_CANDIDATES = new CopyOnWriteArrayList<>(FALLBACK_MODELS);

    public GeminiClientService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * ดึงรายชื่อ model ที่ใช้ได้จาก Google ตอนเริ่มระบบ เก็บเฉพาะ flash/pro ที่ใช้แชทข้อความได้
     * แล้วเรียงตัวที่น่าใช้ที่สุดขึ้นก่อน (ดู modelPriority) ถ้าดึงไม่ได้ใช้ FALLBACK_MODELS
     */
    @PostConstruct
    public void fetchAvailableModels() {
        if (!isApiKeyConfigured()) {
            log.warn("[GeminiClientService] ไม่ได้ตั้ง app.ai.gemini-api-key — แชทจะตอบด้วยกฎสำรองแทน AI");
            return;
        }
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                API_BASE + "?pageSize=1000", HttpMethod.GET, new HttpEntity<>(apiHeaders()), String.class);
            JsonNode modelsNode = objectMapper.readTree(response.getBody()).path("models");

            List<String> fetchedModels = StreamSupport.stream(modelsNode.spliterator(), false)
                .filter(m -> StreamSupport.stream(m.path("supportedGenerationMethods").spliterator(), false)
                        .anyMatch(n -> "generateContent".equals(n.asText())))
                .map(m -> {
                    // name มาในรูป "models/gemini-3.8-flash" — ตัด prefix ออก
                    String fullName = m.path("name").asText("");
                    return fullName.startsWith("models/") ? fullName.substring(7) : fullName;
                })
                .filter(this::isChatModel)
                .sorted(Comparator.comparingInt(this::modelPriority).reversed())
                .collect(Collectors.toList());

            if (!fetchedModels.isEmpty()) {
                MODEL_CANDIDATES = new CopyOnWriteArrayList<>(fetchedModels);
                log.info("[GeminiClientService] โหลด {} model จาก API, ลำดับแรกๆ: {}",
                        fetchedModels.size(), fetchedModels.subList(0, Math.min(5, fetchedModels.size())));
            } else {
                log.warn("[GeminiClientService] API ไม่มี model ที่ใช้แชทได้ — ใช้รายชื่อสำรอง {}", FALLBACK_MODELS);
            }
        } catch (HttpStatusCodeException e) {
            log.error("[GeminiClientService] ดึงรายชื่อ model ไม่ได้ ({}) — ตรวจสอบ app.ai.gemini-api-key: {}",
                    e.getStatusCode(), shortError(e));
        } catch (Exception e) {
            log.warn("[GeminiClientService] ดึงรายชื่อ model ไม่ได้ ({}) — ใช้รายชื่อสำรอง {}",
                    e.getClass().getSimpleName(), FALLBACK_MODELS);
        }
    }

    /** model สำหรับแชทข้อความเท่านั้น — ตัด tts/image/audio ฯลฯ ที่มี generateContent แต่ไม่ได้ตอบเป็นข้อความ */
    private boolean isChatModel(String name) {
        if (!name.startsWith("gemini-") || !(name.contains("flash") || name.contains("pro"))) return false;
        return NON_CHAT_MODEL_KEYWORDS.stream().noneMatch(name::contains);
    }

    /**
     * คะแนนสูง = ลองก่อน — ชนิดมาก่อน (flash > flash-lite > pro) แล้วค่อยดูรุ่น (ใหม่กว่าก่อน)
     * รุ่น preview/exp ถูกหักคะแนนเล็กน้อยให้รุ่นเสถียรรุ่นเดียวกันมาก่อน
     * ชื่อที่ไม่มีเลขรุ่น (เช่น gemini-flash-latest) ไว้ท้ายสุด เพราะไม่รู้ว่าชี้ไปรุ่นไหน
     */
    private int modelPriority(String name) {
        Matcher m = MODEL_NAME.matcher(name);
        if (!m.matches()) return 0;

        int version = Math.round(Float.parseFloat(m.group(1)) * 100); // 3.8 → 380
        boolean lite = m.group(3) != null;
        int kind = "pro".equals(m.group(2)) ? 1 : (lite ? 2 : 3);
        String suffix = m.group(4);
        int unstablePenalty = suffix.contains("preview") || suffix.contains("exp") ? 5 : 0;

        return kind * 10_000 + version - unstablePenalty;
    }

    /**
     * ล้างสัญลักษณ์ markdown ที่หลุดมา — prompt สั่งห้ามแล้ว แต่ model ยังใส่มาบ้างเป็นครั้งคราว
     * หน้าแชทแสดงข้อความดิบ ถ้าไม่ล้าง ลูกค้าจะเห็น ** และ # ติดมาในข้อความ
     */
    static String toChatText(String text) {
        return text
            .replaceAll("\\*\\*|__|`", "")              // ตัวหนา / code
            .replaceAll("(?m)^\\s*#{1,6}\\s+", "")      // หัวข้อ
            .replaceAll("(?m)^\\s*[-*•]\\s+", "")       // bullet
            .replaceAll("(?m)^\\s*\\d+[.)]\\s+", "")   // เลขข้อ
            .replaceAll("\\n{3,}", "\n\n")               // บรรทัดว่างซ้อน
            .trim();
    }

    private boolean isApiKeyConfigured() {
        return geminiApiKey != null && !geminiApiKey.isBlank() && !geminiApiKey.contains("YOUR_GEMINI_API_KEY");
    }

    /**
     * ส่ง key ทาง header ไม่ใส่ใน URL — ถ้าอยู่ใน URL เวลาเกิด error ด้านเครือข่าย
     * RestTemplate จะใส่ URL ทั้งเส้น (รวม key) ไว้ในข้อความ exception แล้วหลุดไปอยู่ใน log
     */
    private HttpHeaders apiHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-goog-api-key", geminiApiKey);
        return headers;
    }

    /** ดึงแค่ข้อความ error จาก Google มาใส่ log ไม่เอา body ทั้งก้อน */
    private String shortError(HttpStatusCodeException e) {
        try {
            String message = objectMapper.readTree(e.getResponseBodyAsString()).path("error").path("message").asText("");
            if (!message.isBlank()) return message.length() > 200 ? message.substring(0, 200) + "…" : message;
        } catch (Exception ignored) {
            // body ไม่ใช่ JSON — ใช้ status อย่างเดียว
        }
        return e.getStatusText();
    }

    private final String SHOPPING_ASSISTANT_PROMPT = """
      คุณคือ SmartSell AI ผู้ช่วยแนะนำการเลือกซื้อเสื้อผ้าส่วนบุคคลของร้านค้ามหาวิทยาลัย UTCC
      หน้าที่หลักของคุณคือ สอบถามความต้องการ (Preferences) ของลูกค้า เพื่อนำไปแนะนำสินค้าที่เหมาะสม
      
      ลำดับการถาม Preference มีเพียง 5 ด้านนี้เท่านั้น (ห้ามถามสีมงคล หรือ lucky_color โดยเด็ดขาด เพราะแยกฟีเจอร์ไปแล้ว):
      1. personal_color (Spring, Summer, Autumn, Winter)
      2. occasion (เช่น ทั่วไป, เข้ากิจกรรม, ทางการ)
      3. budget_min / budget_max (สินค้าในร้านราคา 150 - 250 บาท ตอนถามงบให้เสนอตัวเลือก ไม่เกิน 180 บาท, มากกว่า 200 บาท หรือไม่ระบุ
         ห้ามเสนอช่วงราคาอื่นที่เกิน 250 บาท เช่น 350 หรือ 500
         ไม่เกิน X = budget_min null, budget_max X / มากกว่า X = budget_min X, budget_max null
         ไม่ระบุ หรือไม่จำกัดงบ = budget_min 0, budget_max null — ห้ามคืน null ทั้งคู่ เพราะระบบจะถือว่ายังไม่ได้ตอบ)
      4. size (เช่น S, M, L, XL, XXL)
      5. gender — ถามว่าลูกค้าเป็นผู้ชายหรือผู้หญิง เพื่อเลือกทรงเสื้อให้ถูก ตอนถามให้เสนอตัวเลือก ชาย, หญิง หรือไม่ระบุเพศ
         ชาย = "MALE" / หญิง = "FEMALE" / ไม่ระบุ ไม่อยากบอก หรือดูได้ทุกแบบ = "ANY" — ห้ามคืน null ถ้าลูกค้าตอบแล้ว
         เพราะระบบจะถือว่ายังไม่ได้ตอบ

      กฎสำคัญมาก:
      - ต้องสื่อสารและถาม-ตอบเป็น "ภาษาไทย" เท่านั้น ด้วยน้ำเสียงสุภาพและเป็นกันเอง (ลงท้ายด้วย ค่ะ/นะคะ)
      - ห้ามถามเรื่องสีมงคล หรือ lucky color เป็นอันขาด!
      - ถามเพียง 1 ข้อต่อครั้งตามลำดับข้างต้น
      - อย่าสร้างคำถามนอกเหนือจาก 5 ข้อนี้ (ยกเว้นถามกลับว่าหมายถึงเสื้อตัวไหน ตอนลูกค้าถามเรื่องไซส์)

      - เมื่อได้ข้อมูลครบทั้ง 5 ด้านแล้ว ไม่ต้องถามคำถามเพิ่ม ให้แจ้งลูกค้าว่าได้ข้อมูลครบถ้วนแล้วและกำลังแนะนำสินค้าให้
      - อย่าแนะนำชื่อสินค้าหรือคำนวณคะแนนด้วยตนเอง (ระบบหลังบ้านจะแสดงสินค้าให้เอง)
      - ตอบสั้น 1-2 ประโยค เหมือนพนักงานร้านพิมพ์แชทคุยกับลูกค้า รับคำตอบลูกค้าสั้นๆ ก่อน แล้วค่อยถามข้อต่อไป
      - ห้ามใช้ markdown หรือสัญลักษณ์จัดรูปแบบ (** __ # - * หรือเลขข้อ) ใน reply
      
      คำถามเรื่องร้าน ไซส์ และสต็อก: ลูกค้าถามได้ทุกเมื่อ แม้ยังตอบ 5 ข้อไม่ครบ ให้ตอบตาม "กฎเรื่องร้าน" ท้าย prompt นี้
      และห้ามต่อท้ายคำตอบเรื่องเหล่านี้ด้วยคำถาม preference (Personal Color ฯลฯ)

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
           "size": "ไซส์ หรือ null",
           "gender": "MALE หรือ FEMALE หรือ ANY หรือ null"
        }
      }
      """;

    /**
     * กฎตอบเรื่องร้าน + คำถามนอกเรื่อง ใช้ร่วมกันทั้ง "ผู้ช่วย AI" และ "แชทกับเรา" — ต่อท้าย system prompt ตามด้วยข้อมูลร้าน
     * (StoreInfoService) และตารางไซส์ (ChatService) ที่ดึงใหม่ทุกข้อความ
     */
    private static final String STORE_RULES = """

      กฎเรื่องร้าน ไซส์ และสต็อก:
      - ร้านนี้เป็นเว็บโชว์สินค้า ลูกค้าต้องมาซื้อที่ร้าน ไม่มีตะกร้าหรือสั่งซื้อออนไลน์
      - เรื่องร้าน (ที่ตั้ง เบอร์โทร เวลาเปิด-ปิด และอื่นๆ) ตอบจาก "ข้อมูลร้าน" ท้าย prompt นี้เท่านั้น
      - ถามว่าตอนนี้ร้านเปิดไหม ให้ตอบตาม "สถานะร้านตอนนี้" ห้ามคำนวณเวลาเอง
      - ถ้าสิ่งที่ถามไม่มีในข้อมูลร้าน (เช่น ที่จอดรถ การจัดส่ง ช่องทางชำระเงิน) ให้ตอบว่า
        "ขออภัย ยังไม่มีข้อมูลนี้ กรุณาติดต่อร้านที่เบอร์ <เบอร์โทรจากข้อมูลร้าน>" ห้ามเดา ห้ามแต่งข้อมูลเพิ่ม
      - ถามว่ามีของไหม สต็อกเหลือไหม ไซส์/สีนี้ยังมีไหม ห้ามยืนยันว่ามีหรือหมด
        ให้แนะนำโทรสอบถามร้านที่เบอร์ในข้อมูลร้านก่อนเดินทาง เพราะข้อมูลสต็อกในระบบยังไม่ใช่ของจริง
      - เรื่องไซส์ ใช้ "ตารางไซส์ของสินค้า" ท้าย prompt นี้ ถ้าไม่รู้ว่าลูกค้าหมายถึงเสื้อตัวไหน ให้ถามกลับก่อน
        ถ้าตารางนั้นมีป้าย [ค่าประมาณ ยังไม่ยืนยันจากร้าน] ต้องบอกลูกค้าด้วยว่าตัวเลขเป็นค่าประมาณ
      - ถ้าลูกค้าถามเรื่องที่ไม่เกี่ยวกับสินค้า เสื้อผ้า การแต่งตัว หรือร้าน (เช่น สภาพอากาศ ข่าว การบ้าน คณิตศาสตร์ เรื่องทั่วไป)
        ให้ตอบประโยคนี้เท่านั้น ห้ามตอบเนื้อหาเรื่องนั้น และห้ามเพิ่มอะไรต่อท้าย:
        "ขออภัยค่ะ ไม่มีข้อมูลเรื่องนี้ในฐานระบบข้อมูลของร้านค้าค่ะ"
        (เรื่องที่เกี่ยวกับร้านแต่ไม่มีข้อมูล เช่น ที่จอดรถ ให้ใช้ข้อความที่มีเบอร์โทรด้านบนแทน
        ส่วนคำทักทาย และคำตอบของลูกค้าต่อคำถามที่ผู้ช่วยถามไป เช่น โทนสี โอกาส งบ ไซส์ เพศ ไม่ถือว่านอกเรื่อง)
      - ตอบเฉพาะสิ่งที่ถาม สั้นๆ 1 ประโยค ห้ามแถมข้อมูลร้านเรื่องอื่นที่ไม่ได้ถาม
        ตัวอย่าง: ลูกค้า "ร้านอยู่ไหน" → "ร้านอยู่ที่ตึก 15 ชั้น 1 ใกล้หอประชุมค่ะ"
        (ตัวอย่างนี้แค่รูปแบบ ข้อมูลจริงให้ใช้จากข้อมูลร้านท้าย prompt เสมอ)

      """;

    private static String withStoreContext(String prompt, String storeContext) {
        return storeContext == null ? prompt : prompt + STORE_RULES + storeContext;
    }

    /** ตอบเมื่อเรียก Gemini ไม่สำเร็จ — คนละข้อความกับ "ไม่มีข้อมูลในระบบ" ที่ AI ตอบเองตอนหาไม่เจอ */
    static final String AI_UNAVAILABLE_REPLY =
        "ขออภัยค่ะ ระบบ AI ขัดข้องชั่วคราว ยังตอบคำถามไม่ได้ในตอนนี้ กรุณาลองใหม่อีกครั้งในอีกสักครู่นะคะ";

    private final String PRODUCT_CHATBOT_PROMPT = """
      คุณคือ SmartSell AI ทำหน้าที่เหมือนพนักงานหน้าร้านของร้านค้ามหาวิทยาลัย UTCC ที่คอยตอบแชทลูกค้า 24 ชม.
      คุยกับลูกค้าเหมือนพนักงานร้านที่เป็นกันเองกำลังพิมพ์แชทตอบ ไม่ใช่เหมือนอ่านเอกสารหรือรายงาน

      กฎเรื่องข้อมูล (สำคัญที่สุด ห้ามฝ่าฝืน):
      1. ตอบจากข้อมูลใน PRODUCT CONTEXT (ข้อมูลจากฐานข้อมูลของร้าน) และข้อมูลร้าน/ตารางไซส์ท้าย prompt นี้เท่านั้น ห้ามแต่งเติม ห้ามเดาราคา สี ไซส์ หรือสินค้าที่ไม่มีในข้อมูล
      2. ถ้าลูกค้าถามสิ่งที่ไม่มีในข้อมูล ให้บอกตรงๆ อย่างสุภาพว่าทางร้านยังไม่มีข้อมูลหรือสินค้านั้น
         ถ้าในข้อมูลมีสินค้าที่ใกล้เคียงจริงๆ เสนอได้สั้นๆ แต่ถ้าไม่มี ไม่ต้องเสนออะไร

      วิธีพูด:
      - ภาษาไทย สุภาพแต่เป็นกันเอง ลงท้าย ค่ะ/นะคะ ตามธรรมชาติ ไม่ต้องลงท้ายทุกประโยค
      - ตอบสั้น 1-3 ประโยค ตอบตรงสิ่งที่ถามก่อน แล้วค่อยเสริมเฉพาะที่จำเป็น
      - ถ้าข้อมูลเยอะ (เช่น สีหรือรุ่นหลายแบบ) ให้สรุปภาพรวม แล้วถามต่อว่าลูกค้าสนใจแบบไหน แทนการไล่ครบทุกรายการ
      - ห้ามใช้ markdown หรือสัญลักษณ์จัดรูปแบบ เช่น ** __ # - * หรือเลขข้อ 1. 2. ให้เขียนเป็นประโยคต่อเนื่องเหมือนพิมพ์แชท
      - ถ้าต้องบอกหลายอย่างจริงๆ คั่นด้วยการขึ้นบรรทัดใหม่ได้ แต่ไม่ต้องใส่สัญลักษณ์นำหน้า
      - ไม่ต้องขึ้นต้นด้วยการทักทายซ้ำทุกข้อความ และไม่ต้องพูดว่า "จากข้อมูลในระบบ" หรือ "ตามฐานข้อมูล"

      ตัวอย่างน้ำเสียง:
      ลูกค้า: มีเสื้อโปโลสีอะไรบ้าง
      ตอบ: โปโลของร้านมีทั้งแบบชายและหญิงค่ะ ตัวละ 250 บาท มีสีขาว ดำ กรม เหลือง และเทาให้เลือก สนใจสีไหนเป็นพิเศษไหมคะ เดี๋ยวเช็คไซส์ให้ค่ะ
      ลูกค้า: ร้านขายรองเท้าไหม
      ตอบ: ตอนนี้ร้านยังไม่มีรองเท้าขายนะคะ จะมีแต่เสื้อผ้าเป็นหลักค่ะ
      """;

    /** storeContext = ข้อมูลร้าน เวลาเปิด เวลาตอนนี้ และตารางไซส์ (จาก ChatService) ต่อท้าย system prompt */
    public AssistantResponse generateAssistantReply(List<ChatMessage> history, String newMessage, String storeContext) {
        String systemPrompt = withStoreContext(SHOPPING_ASSISTANT_PROMPT, storeContext);
        String jsonResponseStr = callGeminiAPI(systemPrompt, history, newMessage, null);
        
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
                resp.setReply(toChatText(reply));
                
                if (prefs != null) {
                    if (prefs.has("personal_color") && !prefs.get("personal_color").isNull()) resp.setPersonalColor(prefs.get("personal_color").asText());
                    if (prefs.has("occasion") && !prefs.get("occasion").isNull()) resp.setOccasion(prefs.get("occasion").asText());
                    if (prefs.has("size") && !prefs.get("size").isNull()) resp.setSize(prefs.get("size").asText());
                    if (prefs.has("budget_min") && !prefs.get("budget_min").isNull()) resp.setBudgetMin(prefs.get("budget_min").asDouble());
                    if (prefs.has("budget_max") && !prefs.get("budget_max").isNull()) resp.setBudgetMax(prefs.get("budget_max").asDouble());
                    if (prefs.has("gender") && !prefs.get("gender").isNull()) resp.setGender(prefs.get("gender").asText());
                }
                
                // If reply is valid and in Thai
                if (resp.getReply() != null && !resp.getReply().isEmpty()) {
                    return resp;
                }
            } catch (Exception e) {
                log.warn("[GeminiClientService] แปลงคำตอบของ AI เป็น JSON ไม่ได้ ({}) — ใช้กฎสำรองแทน",
                        e.getClass().getSimpleName());
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
        } else if (lower.contains("ไม่รู้") || lower.contains("ข้าม")) {
            resp.setPersonalColor("Unknown");
            resp.setReply("ไม่เป็นไรเลยค่ะ! เราสามารถข้ามไปคำถามถัดไปได้เลย แล้วคุณอยากได้ชุดสำหรับใส่ในโอกาสไหนคะ? (เช่น ทั่วไป, เข้ากิจกรรม, ทางการ)");
            return resp;
        }
        
        // 5. Gender — เช็คก่อนงบ เพราะ "ไม่ระบุ" ชนกับคำตอบเรื่องงบ (ปุ่มตอบด่วนจึงใช้ "ไม่ระบุเพศ")
        //    และเช็ค "หญิง" ก่อน "ชาย" เพราะ "ผู้หญิง" ไม่มีคำว่าชาย แต่ทั้งคู่ต้องจับให้ได้
        String gender = lower.contains("ไม่ระบุเพศ") ? "ANY"
                : lower.contains("หญิง") ? "FEMALE"
                : lower.contains("ชาย") ? "MALE"
                : null;
        if (gender != null) {
            resp.setGender(gender);
            resp.setReply("รับทราบค่ะ ได้ข้อมูลครบถ้วนแล้ว ระบบได้คัดเลือกสินค้าที่เหมาะกับคุณที่สุดมาให้ชมด้านล่างนะคะ ✨");
            return resp;
        }

        // 2. Occasion
        if (lower.contains("กิจกรรม") || lower.contains("ทั่วไป") || lower.contains("ทางการ") || lower.contains("เรียน") || lower.contains("เที่ยว") || lower.contains("ทำงาน") || lower.contains("casual") || lower.contains("formal")) {
            resp.setOccasion(newMessage.trim());
            resp.setReply("บันทึกโอกาสการใช้งานเรียบร้อยค่ะ! คุณมีงบประมาณสำหรับการแต่งตัวครั้งนี้เท่าไหร่คะ? (เช่น ไม่เกิน 180, มากกว่า 200, ไม่ระบุ)");
            return resp;
        }

        // 3. Budget — ตัวเลขแรกในข้อความคืองบ: "มากกว่า 200" เป็นงบขั้นต่ำ นอกนั้นเป็นงบสูงสุด (เช่น "ไม่เกิน 180")
        if (lower.contains("ไม่ระบุ")) {
            resp.setBudgetMin(0.0);
            resp.setBudgetMax(99999.0);
            resp.setReply("รับทราบค่ะ ไม่จำกัดงบประมาณ คุณสวมเสื้อผ้าไซส์ไหนเป็นประจำคะ? (S, M, L, XL, XXL)");
            return resp;
        }
        java.util.regex.Matcher budget = java.util.regex.Pattern.compile("\\d+").matcher(lower);
        if (budget.find()) {
            double amount = Double.parseDouble(budget.group());
            if (lower.contains("มากกว่า")) {
                resp.setBudgetMin(amount);
                resp.setReply("บันทึกงบประมาณมากกว่า " + budget.group() + " บาทเรียบร้อยค่ะ! คุณสวมเสื้อผ้าไซส์ไหนเป็นประจำคะ? (S, M, L, XL, XXL)");
            } else {
                resp.setBudgetMin(0.0);
                resp.setBudgetMax(amount);
                resp.setReply("บันทึกงบประมาณไม่เกิน " + budget.group() + " บาทเรียบร้อยค่ะ! คุณสวมเสื้อผ้าไซส์ไหนเป็นประจำคะ? (S, M, L, XL, XXL)");
            }
            return resp;
        }
        
        // 4. Size → ถามเพศเป็นข้อสุดท้าย (no lucky color question!)
        if (lower.equals("s") || lower.equals("m") || lower.equals("l") || lower.equals("xl") || lower.equals("xxl")) {
            String size = lower.toUpperCase();
            resp.setSize(size);
            resp.setReply("บันทึกไซส์ " + size + " ให้เรียบร้อยค่ะ! ข้อสุดท้าย คุณเป็นผู้ชายหรือผู้หญิงคะ จะได้เลือกทรงเสื้อให้ถูก (ชาย, หญิง, ไม่ระบุเพศ)");
            return resp;
        }
        
        // Default welcome in Thai
        resp.setReply("ยินดีต้อนรับสู่ SmartSell AI ค่ะ! เพื่อช่วยแนะนำชุดที่เหมาะกับคุณที่สุด ขอเริ่มถาม Personal Color ก่อนนะคะ — โทนสีของคุณเป็นแบบไหน? (Spring, Summer, Autumn, Winter)");
        return resp;
    }

    public String generateSupportReply(List<ChatMessage> history, String newMessage, String productContext, String storeContext) {
        String reply = callGeminiAPI(withStoreContext(PRODUCT_CHATBOT_PROMPT, storeContext), history, newMessage, productContext);
        if (reply != null && !reply.isBlank()) {
            return toChatText(reply);
        }
        // ต้องต่างจากข้อความ "ไม่มีข้อมูลในระบบ" ที่ prompt สั่งให้ AI ใช้ ไม่งั้นแยกไม่ออกว่า AI ล่มหรือหาข้อมูลไม่เจอ
        return AI_UNAVAILABLE_REPLY;
    }

    /**
     * ส่งข้อความไปให้ Gemini ไล่ลองทีละ model ตามลำดับใน MODEL_CANDIDATES
     * คืน null เมื่อไม่มี model ไหนตอบได้ — ผู้เรียกต้องมีคำตอบสำรองเอง
     */
    private String callGeminiAPI(String systemPrompt, List<ChatMessage> history, String newMessage, String context) {
        if (!isApiKeyConfigured()) {
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
                
                HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, apiHeaders());

                String url = API_BASE + "/" + modelName + ":generateContent";
                String responseStr = restTemplate.postForObject(url, entity, String.class);
                
                // Parse response safely
                JsonNode rootNode = objectMapper.readTree(responseStr);
                JsonNode candidates = rootNode.path("candidates");
                if (candidates.isArray() && candidates.size() > 0) {
                    JsonNode firstCandidate = candidates.get(0);
                    // Check finish reason - skip if SAFETY or RECITATION
                    String finishReason = firstCandidate.path("finishReason").asText("");
                    if ("SAFETY".equals(finishReason) || "RECITATION".equals(finishReason)) {
                        log.info("[GeminiClientService] {} ไม่ตอบ (finishReason={}) — ลอง model ถัดไป", modelName, finishReason);
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
                log.warn("[GeminiClientService] {} ตอบกลับมาว่าง — ลอง model ถัดไป", modelName);

            } catch (HttpStatusCodeException e) {
                int status = e.getStatusCode().value();
                if (status == 401 || status == 403) {
                    // key ผิด/ถูกบล็อก — ทุก model จะได้ผลเดียวกัน ลองต่อไปก็เสียเวลาเปล่า
                    log.error("[GeminiClientService] Gemini ปฏิเสธ API key ({}): {} — ตรวจสอบ app.ai.gemini-api-key",
                            status, shortError(e));
                    return null;
                }
                if (status == 404) {
                    // model ถูกปลดหรือปิดสำหรับบัญชีนี้ — ตัดทิ้ง ไม่ต้องลองซ้ำทุกข้อความ
                    MODEL_CANDIDATES.remove(modelName);
                    log.warn("[GeminiClientService] ตัด {} ออกจากรายชื่อ (404): {}", modelName, shortError(e));
                } else {
                    log.warn("[GeminiClientService] {} ตอบ {}: {} — ลอง model ถัดไป", modelName, status, shortError(e));
                }
            } catch (Exception e) {
                log.warn("[GeminiClientService] เรียก {} ไม่สำเร็จ ({}) — ลอง model ถัดไป",
                        modelName, e.getClass().getSimpleName());
            }
        }
        log.error("[GeminiClientService] ไม่มี model ไหนตอบได้ (ลองแล้ว {} ตัว)", MODEL_CANDIDATES.size());
        return null;
    }

    public static class AssistantResponse {
        private String reply;
        private String personalColor;
        private String occasion;
        private Double budgetMin;
        private Double budgetMax;
        private String size;
        private String gender;

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
        public String getGender() { return gender; }
        public void setGender(String gender) { this.gender = gender; }
    }
}
