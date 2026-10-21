package com.smartsell.dto;

import java.math.BigDecimal;
import java.util.List;

public class PersonalColorDTO {

    // sent from the frontend when the customer picks a season themselves
    public record ManualRequest(String sessionToken, String season) {}

    // sent from the frontend after the quiz; answers are the chosen axes in question order
    // (warm/cool, warm/cool, light/deep, warm/cool, deep/light)
    public record QuizRequest(String sessionToken, List<String> answers) {}

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
