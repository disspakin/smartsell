package com.smartsell.dto;

import java.math.BigDecimal;
import java.util.List;

public class PersonalColorDTO {

    // sent from the frontend when the customer picks a season themselves
    public record ManualRequest(String sessionToken, String season) {}

    // sent from the frontend when the customer uploads a photo (Gemini not wired up yet — see PersonalColorService)
    public record PhotoRequest(String sessionToken, String imageBase64) {}

    // what both endpoints return — enough to render the result card + palette
    public record Result(
            Long id,
            String season,
            BigDecimal confidence,   // null for MANUAL
            String reasoning,
            List<PaletteColor> palette
    ) {}

    public record PaletteColor(String name, String hex) {}
}
