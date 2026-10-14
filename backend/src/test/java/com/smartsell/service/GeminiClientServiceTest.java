package com.smartsell.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GeminiClientService")
class GeminiClientServiceTest {

    @Test
    @DisplayName("ล้าง markdown ที่ AI ใส่มา ให้เหลือข้อความแชทธรรมดา")
    void toChatTextStripsMarkdown() {
        String raw = "## สีที่มี\n**เสื้อโปโล หญิง** มี 5 สี\n- สีขาว\n* สีดำ\n1. สีกรม\n2) สีเทา\n\n\n\nสนใจสีไหนคะ";

        assertThat(GeminiClientService.toChatText(raw))
                .isEqualTo("สีที่มี\nเสื้อโปโล หญิง มี 5 สี\nสีขาว\nสีดำ\nสีกรม\nสีเทา\n\nสนใจสีไหนคะ");
    }

    @Test
    @DisplayName("ไม่ไปแตะตัวเลขที่เป็นเนื้อหา เช่น ราคาหรือทศนิยม")
    void toChatTextKeepsNumbers() {
        String raw = "250 บาทค่ะ\n1.5 เมตร\n3XL ราคา 170 บาท";

        assertThat(GeminiClientService.toChatText(raw)).isEqualTo(raw);
    }

    // ไม่ได้ตั้ง API key — generateAssistantReply ตกไปใช้กฎสำรองเสมอ
    private GeminiClientService.AssistantResponse fallback(String message) {
        return new GeminiClientService().generateAssistantReply(List.of(), message, null);
    }

    @Test
    @DisplayName("กฎสำรอง: ถามเพศต่อหลังได้ไซส์ และจับคำตอบเพศได้")
    void fallbackAsksAndParsesGender() {
        assertThat(fallback("M").getReply()).contains("ผู้ชายหรือผู้หญิง");

        assertThat(fallback("หญิง").getGender()).isEqualTo("FEMALE");
        assertThat(fallback("ผู้ชายครับ").getGender()).isEqualTo("MALE");
        assertThat(fallback("ไม่ระบุเพศ").getGender()).isEqualTo("ANY");
    }

    @Test
    @DisplayName("กฎสำรอง: \"ไม่ระบุ\" เฉยๆ ยังเป็นคำตอบเรื่องงบ ไม่ใช่เพศ")
    void fallbackKeepsBudgetNotSpecified() {
        GeminiClientService.AssistantResponse resp = fallback("ไม่ระบุ");

        assertThat(resp.getGender()).isNull();
        assertThat(resp.getBudgetMin()).isEqualTo(0.0);
    }
}
