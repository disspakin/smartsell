package com.smartsell.dto;

import java.util.List;

public class ChatDTO {

    // what the frontend sends when the customer types/answers a chip
    public record ChatRequest(Long sessionId, String message) {}

    // what comes back to render as the next AI bubble + updated preference summary + top 3 recommended products
    public record ChatResponse(Long sessionId, String reply, PreferenceSummary preferences, List<ProductDTO> products) {}

    public record PreferenceSummary(String personalColor, String occasion, String budget, String size) {}
}
