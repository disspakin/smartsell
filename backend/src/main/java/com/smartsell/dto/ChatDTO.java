package com.smartsell.dto;

public class ChatDTO {

    // what the frontend sends when the customer types/answers a chip
    public record ChatRequest(Long sessionId, String message) {}

    // what comes back to render as the next AI bubble + updated preference summary
    public record ChatResponse(Long sessionId, String reply, PreferenceSummary preferences) {}

    public record PreferenceSummary(String personalColor, String occasion, String budget, String size) {}
}
