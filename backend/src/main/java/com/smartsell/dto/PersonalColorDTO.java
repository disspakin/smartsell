package com.smartsell.dto;

import java.math.BigDecimal;
import java.util.List;

public class PersonalColorDTO {

    // sent from the frontend when the customer picks a season themselves
    public record ManualRequest(String sessionToken, String season) {}

    // what both endpoints return — enough to render the result card + palette + recommended products
    public record Result(
            Long id,
            String season,
            BigDecimal confidence,   // null for MANUAL
            String reasoning,
            List<PaletteColor> palette,
            List<ProductDTO> products  // top 3 recommended products based on rulebase scoring
    ) {}

    public record PaletteColor(String name, String hex) {}
}
