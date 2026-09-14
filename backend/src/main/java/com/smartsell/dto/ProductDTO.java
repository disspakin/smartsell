package com.smartsell.dto;

import java.math.BigDecimal;
import java.util.List;

// what the frontend actually receives — flatter and safer to expose than the JPA entity directly
public record ProductDTO(
        Long id,
        String name,
        String category,
        BigDecimal price,
        String description,
        String imageUrl,
        List<VariantDTO> variants
) {
    public record VariantDTO(Long id, String color, String colorHex, String size, String imageUrl) {}
}
