package com.smartsell.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ChatDTO.ChatRequest")
class ChatDTOTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private boolean isValid(String message) {
        return validator.validate(new ChatDTO.ChatRequest(null, message)).isEmpty();
    }

    @Test
    @DisplayName("รับข้อความยาวได้ถึงขีดจำกัดพอดี")
    void acceptsUpToLimit() {
        assertThat(isValid("สวัสดี")).isTrue();
        assertThat(isValid("ก".repeat(ChatDTO.MAX_MESSAGE_LENGTH))).isTrue();
    }

    @Test
    @DisplayName("ไม่รับข้อความที่ยาวเกิน ว่าง หรือเป็น null")
    void rejectsTooLongOrBlank() {
        assertThat(isValid("ก".repeat(ChatDTO.MAX_MESSAGE_LENGTH + 1))).isFalse();
        assertThat(isValid("   ")).isFalse();
        assertThat(isValid(null)).isFalse();
    }
}
