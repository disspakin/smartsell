package com.smartsell.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public class ChatDTO {

    // ข้อความยาวกิน token ของ Gemini — ฝั่งหน้าเว็บตั้ง maxLength เท่ากันใน Assistant.jsx และ ChatSupport.jsx
    public static final int MAX_MESSAGE_LENGTH = 500;

    // what the frontend sends when the customer types/answers a chip
    public record ChatRequest(
            Long sessionId,
            @NotBlank(message = "กรุณาพิมพ์ข้อความ")
            @Size(max = MAX_MESSAGE_LENGTH, message = "ข้อความยาวเกิน 500 ตัวอักษร")
            String message) {}

    // what comes back to render as the next AI bubble + updated preference summary + top 3 recommended products
    public record ChatResponse(Long sessionId, String reply, PreferenceSummary preferences, List<ProductDTO> products) {}

    // gender: MALE / FEMALE / ANY (ไม่ระบุ) หรือ null ถ้ายังไม่ได้ถาม
    public record PreferenceSummary(String personalColor, String occasion, String budget, String size, String gender) {}
}
