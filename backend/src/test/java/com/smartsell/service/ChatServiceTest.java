package com.smartsell.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ChatService")
class ChatServiceTest {

    @Test
    @DisplayName("XXL, XXXL, XXXXL แปลงเป็น 2XL, 3XL, 4XL ให้ตรงกับไซส์ใน DB")
    void normalizeSizeMapsXxlToNumbered() {
        assertThat(ChatService.normalizeSize("XXL")).isEqualTo("2XL");
        assertThat(ChatService.normalizeSize("xxxl")).isEqualTo("3XL");
        assertThat(ChatService.normalizeSize("XXXXL")).isEqualTo("4XL");
    }

    @Test
    @DisplayName("ไซส์ที่ Gemini คืนมาซ้ำติดกัน เก็บแค่ชุดแรก")
    void normalizeSizeCollapsesRepeats() {
        assertThat(ChatService.normalizeSize("SSSS")).isEqualTo("SS");
        assertThat(ChatService.normalizeSize("XXXLXXXL")).isEqualTo("3XL");
        assertThat(ChatService.normalizeSize("2XL2XL")).isEqualTo("2XL");
        assertThat(ChatService.normalizeSize("XLXL")).isEqualTo("XL");
    }

    @Test
    @DisplayName("ไซส์อื่นแค่ตัดช่องว่างและทำเป็นตัวพิมพ์ใหญ่")
    void normalizeSizeKeepsOtherSizes() {
        assertThat(ChatService.normalizeSize(" xl ")).isEqualTo("XL");
        assertThat(ChatService.normalizeSize("2xl")).isEqualTo("2XL");
        assertThat(ChatService.normalizeSize("SS")).isEqualTo("SS");
        assertThat(ChatService.normalizeSize("  ")).isNull();
        assertThat(ChatService.normalizeSize(null)).isNull();
    }
}
