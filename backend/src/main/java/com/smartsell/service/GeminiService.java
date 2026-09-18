package com.smartsell.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

import java.util.List;
import java.util.Map;

@Service
public class GeminiService {

    @Value("${app.ai.gemini-api-key}")
    private String apiKey;

    private final RestTemplate restTemplate;

    // สลับลำดับ: เอาโมเดลรุ่นระบุเวอร์ชันชัดเจน (เสถียรกว่า) ขึ้นก่อน
    // ตัว "3.6-flash" เป็นรุ่นใหม่ล่าสุด คนแห่ใช้เยอะช่วงนี้ เลยเก็บไว้ท้ายสุดแทน
    private static final String[] MODELS_TO_TRY = {
        "gemini-3.5-flash",
        "gemini-3.6-flash",
        "gemini-2.0-flash-001"
    };

    private static final int RETRIES_PER_MODEL = 2;

    public GeminiService() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(15000);
        this.restTemplate = new RestTemplate(factory);
    }

    public String ask(String systemInstruction, String userMessage) {
        Exception lastError = null;

        for (String model : MODELS_TO_TRY) {
            for (int attempt = 1; attempt <= RETRIES_PER_MODEL; attempt++) {
                try {
                    return callGemini(model, systemInstruction, userMessage);

                } catch (HttpServerErrorException.ServiceUnavailable e) {
                    lastError = e;
                    System.out.println("โมเดล " + model + " ไม่ว่าง (ครั้งที่ " + attempt + ")");
                    try {
                        Thread.sleep(800L * attempt);
                    } catch (InterruptedException ignored) {}

                } catch (org.springframework.web.client.HttpClientErrorException.NotFound e) {
                    System.out.println("โมเดล " + model + " ไม่พบ ข้ามไปตัวถัดไป");
                    lastError = e;
                    break;

                } catch (Exception e) {
                    e.printStackTrace();
                    return "ขออภัยค่ะ ระบบ AI ขัดข้องชั่วคราว ลองใหม่อีกครั้งนะคะ";
                }
            }
        }

        if (lastError != null) lastError.printStackTrace();
        return "ตอนนี้ระบบ AI มีผู้ใช้งานเยอะเป็นพิเศษค่ะ รบกวนลองส่งข้อความอีกครั้งใน 1 นาทีนะคะ";
    }

    private String callGemini(String model, String systemInstruction, String userMessage) {
        String url = "https://generativelanguage.googleapis.com/v1beta/models/"
                + model + ":generateContent?key=" + apiKey;

        Map<String, Object> body = Map.of(
                "system_instruction", Map.of(
                        "parts", List.of(Map.of("text", systemInstruction))
                ),
                "contents", List.of(
                        Map.of("role", "user", "parts", List.of(Map.of("text", userMessage)))
                ),
                "generationConfig", Map.of(
                        // เพิ่มจาก 200 เป็น 400 กันตัดกลางประโยคจนอ่านไม่รู้เรื่อง
                        "maxOutputTokens", 2048,
                        "temperature", 0.6
                )
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);

        Map responseBody = response.getBody();
        List candidates = (List) responseBody.get("candidates");
        Map firstCandidate = (Map) candidates.get(0);
        Map content = (Map) firstCandidate.get("content");
        List parts = (List) content.get("parts");
        Map firstPart = (Map) parts.get(0);
        return (String) firstPart.get("text");
    }
}