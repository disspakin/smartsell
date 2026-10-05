package com.smartsell.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

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
    // ถ้า product ไม่มีรูปของตัวเอง ใช้รูปของ variant แรกที่มีรูปแทน
    public ProductDTO {
        if (imageUrl == null && variants != null) {
            imageUrl = variants.stream()
                    .map(VariantDTO::imageUrl)
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(null);
        }
    }

    public record VariantDTO(Long id, String color, String colorHex, String size, String imageUrl) {}
}