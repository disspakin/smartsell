package com.smartsell.service;

import com.smartsell.dto.ProductDTO;
import com.smartsell.entity.Product;
import com.smartsell.entity.ProductVariant;
import com.smartsell.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Scoring engine for Personal Color recommendations.
 * Pipeline: filter (none for now) → score by color + occasion → sort → top N
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
     *   Occasion match             : 3 = matched category, 0 = not matched
     */
    public List<ProductDTO> recommendByPersonalColor(String season, String occasion, int topN) {
        List<String> targetColors = colorRuleEngine.getPersonalColorList(season);
        List<Product> allProducts = productRepository.findAllWithVariants();

        List<ScoredProduct> scored = allProducts.stream()
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
                .map(sp -> toDTO(sp.product()))
                .collect(Collectors.toList());
    }

    // ── helpers ──────────────────────────────────────────────────────────

    private int bestVariantColorScore(Product p, List<String> targetColors) {
        return p.getVariants().stream()
                .mapToInt(v -> colorRuleEngine.scoreColor(v.getColor(), targetColors))
                .max()
                .orElse(1);
    }

    private ProductDTO toDTO(Product p) {
        List<ProductDTO.VariantDTO> variants = p.getVariants().stream()
                .map(v -> new ProductDTO.VariantDTO(
                        v.getId(), v.getColor(), v.getColorHex(), v.getSize(), v.getImageUrl()))
                .collect(Collectors.toList());
        return new ProductDTO(
                p.getId(), p.getName(), p.getCategory(), p.getPrice(),
                p.getDescription(), p.getImageUrl(), variants);
    }

    private record ScoredProduct(Product product, int score) {}
}
