package com.smartsell.service;

import com.smartsell.dto.ProductDTO;
import com.smartsell.entity.Product;
import com.smartsell.entity.ProductVariant;
import com.smartsell.entity.TargetGroup;
import com.smartsell.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Scoring engine for Personal Color recommendations.
 * Pipeline: filter (gender + budget + size) → score by color + occasion → sort → top N
 */
@Service
public class ProductScoringService {

    private final ProductRepository productRepository;
    private final ColorRuleEngine colorRuleEngine;
    private final OccasionRuleEngine occasionRuleEngine;

    public ProductScoringService(ProductRepository productRepository,
                                  ColorRuleEngine colorRuleEngine,
                                  OccasionRuleEngine occasionRuleEngine) {
        this.productRepository = productRepository;
        this.colorRuleEngine = colorRuleEngine;
        this.occasionRuleEngine = occasionRuleEngine;
    }

    /**
     * Return top N products scored by personal color season (and optionally occasion).
     *
     * Scoring:
     *   Color match (best variant) : 3 = exact/alias, 2 = partial, 1 = fallback
     *   Occasion match             : 0–3 by category (see OccasionRuleEngine)
     *
     * gender (MALE / FEMALE / ANY / null) เป็นตัวกรอง ไม่ใช่คะแนน — เสื้อผิดเพศต้องไม่หลุดมาเลย
     * เสื้อเด็กไม่ถูกแนะนำทุกกรณี (ดู TargetGroup.suitsAdult)
     */
    public List<ProductDTO> recommendByPersonalColor(String season, String occasion, java.math.BigDecimal budgetMin,
                                                     java.math.BigDecimal budgetMax, String size, String gender,
                                                     int topN) {
        if (season == null && occasion == null) {
            return List.of();
        }
        List<String> targetColors = colorRuleEngine.getPersonalColorList(season);
        List<Product> allProducts = productRepository.findAllWithVariants();

        List<ScoredProduct> scored = allProducts.stream()
                .filter(p -> TargetGroup.suitsAdult(p.getTargetGroup(), gender))
                .filter(p -> budgetMin == null || p.getPrice().compareTo(budgetMin) >= 0)
                .filter(p -> budgetMax == null || p.getPrice().compareTo(budgetMax) <= 0)
                .filter(p -> size == null || p.getVariants().stream().anyMatch(v -> size.equalsIgnoreCase(v.getSize())))
                .map(p -> {
                    int colorScore = bestVariantColorScore(p, targetColors);
                    int occasionScore = (occasion != null)
                            ? occasionRuleEngine.score(occasion, p.getCategory())
                            : 0;
                    return new ScoredProduct(p, colorScore + occasionScore);
                })
                .sorted(Comparator.comparingInt(ScoredProduct::score).reversed())
                .limit(topN)
                .collect(Collectors.toList());

        return scored.stream()
                .map(sp -> toDTO(sp.product(), targetColors, size))
                .collect(Collectors.toList());
    }

    // ── helpers ──────────────────────────────────────────────────────────

    private int bestVariantColorScore(Product p, List<String> targetColors) {
        return p.getVariants().stream()
                .mapToInt(v -> colorRuleEngine.scoreColor(v.getColor(), targetColors))
                .max()
                .orElse(1);
    }

    /**
     * แสดงผลเท่านั้น ไม่มีผลกับการให้คะแนน: สีของสินค้าที่ scoreColor บอกว่าเข้ากับ personal color
     * (สีละหนึ่ง variant เฉพาะสีที่มีไซส์ที่ลูกค้าเลือก) เรียงจากเข้าที่สุด และใช้รูปของสีแรกเป็นรูปการ์ด
     */
    private ProductDTO toDTO(Product p, List<String> targetColors, String size) {
        List<ProductDTO.VariantDTO> variants = p.getVariants().stream()
                .map(ProductDTO.VariantDTO::from)
                .collect(Collectors.toList());

        Map<String, ProductVariant> byColor = new LinkedHashMap<>();
        p.getVariants().stream()
                .filter(v -> v.getColor() != null)
                .filter(v -> size == null || size.equalsIgnoreCase(v.getSize()))
                .sorted(Comparator.comparing(ProductVariant::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                .forEach(v -> byColor.putIfAbsent(v.getColor(), v));
        List<ProductDTO.MatchedColor> matchedColors = byColor.values().stream()
                .filter(v -> colorRuleEngine.scoreColor(v.getColor(), targetColors) > 1)
                .sorted(Comparator.comparingInt((ProductVariant v) -> colorRuleEngine.scoreColor(v.getColor(), targetColors)).reversed())
                .map(v -> new ProductDTO.MatchedColor(v.getColor(), v.getColorHex(),
                        v.getImageUrl() != null ? v.getImageUrl() : p.getImageUrl()))
                .collect(Collectors.toList());

        String imageUrl = matchedColors.isEmpty() ? p.getImageUrl() : matchedColors.get(0).imageUrl();
        return new ProductDTO(
                p.getId(), p.getName(), p.getCategory(), p.getPrice(),
                p.getDescription(), imageUrl, variants, matchedColors, p.getTargetGroup());
    }

    private record ScoredProduct(Product product, int score) {}
}
