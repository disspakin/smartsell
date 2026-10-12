package com.smartsell.dto;

import com.smartsell.entity.ProductVariant;

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
        List<VariantDTO> variants,
        // เฉพาะการแนะนำตาม personal color: สีของสินค้านี้ที่เข้ากับลูกค้า เรียงจากเข้าที่สุด (null ในที่อื่น)
        List<MatchedColor> matchedColors,
        // ช่วงราคาจาก effectivePrice ของทุก variant — หน้ารายการแสดง "150 – 180 บาท" เมื่อไม่เท่ากัน
        BigDecimal minPrice,
        BigDecimal maxPrice
) {
    public ProductDTO(Long id, String name, String category, BigDecimal price, String description,
                      String imageUrl, List<VariantDTO> variants) {
        this(id, name, category, price, description, imageUrl, variants, null);
    }

    public ProductDTO(Long id, String name, String category, BigDecimal price, String description,
                      String imageUrl, List<VariantDTO> variants, List<MatchedColor> matchedColors) {
        this(id, name, category, price, description, imageUrl, variants, matchedColors, null, null);
    }

    // ถ้า product ไม่มีรูปของตัวเอง ใช้รูปของ variant แรกที่มีรูปแทน
    // min/max คำนวณจาก variants เสมอ สินค้าที่ไม่มี variant ใช้ราคาปกติทั้งคู่
    public ProductDTO {
        if (imageUrl == null && variants != null) {
            imageUrl = variants.stream()
                    .map(VariantDTO::imageUrl)
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(null);
        }
        List<BigDecimal> prices = variants == null ? List.of()
                : variants.stream().map(VariantDTO::effectivePrice).filter(Objects::nonNull).toList();
        minPrice = prices.stream().min(BigDecimal::compareTo).orElse(price);
        maxPrice = prices.stream().max(BigDecimal::compareTo).orElse(price);
    }

    public record MatchedColor(String color, String colorHex, String imageUrl) {}

    public record VariantDTO(Long id, String color, String colorHex, String size, String imageUrl, int stockQty,
                             BigDecimal effectivePrice) {

        public static VariantDTO from(ProductVariant v) {
            return new VariantDTO(v.getId(), v.getColor(), v.getColorHex(), v.getSize(), v.getImageUrl(),
                    v.getStockQty(), v.getEffectivePrice());
        }
    }
}