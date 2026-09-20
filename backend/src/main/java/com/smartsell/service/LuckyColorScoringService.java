package com.smartsell.service;

import com.smartsell.dto.ProductDTO;
import com.smartsell.entity.Product;
import com.smartsell.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Scoring engine for Lucky Color recommendations (independent from Personal Color).
 *
 * Scoring:
 *   Combined lucky score = day color score + goal color score
 *   day color   : 3 = exact/alias, 2 = partial, 1 = fallback
 *   goal color  : 3 = exact/alias, 2 = partial, 0 = no match (goal is optional)
 */
@Service
public class LuckyColorScoringService {

    private final ProductRepository productRepository;
    private final ColorRuleEngine colorRuleEngine;

    public LuckyColorScoringService(ProductRepository productRepository,
                                     ColorRuleEngine colorRuleEngine) {
        this.productRepository = productRepository;
        this.colorRuleEngine = colorRuleEngine;
    }

    /**
     * Return top N products scored by lucky color (birth day + goal).
     *
     * @param birthWeekday วันเกิด (จันทร์, อังคาร, ...)
     * @param luckyGoal    เสริมด้านไหน (การเรียน, การงาน, ...)
     * @param topN         จำนวนสินค้าที่ return
     */
    public List<ProductDTO> recommend(String birthWeekday, String luckyGoal, int topN) {
        List<String> dayColors  = colorRuleEngine.getLuckyColorsByDay(birthWeekday);
        List<String> goalColors = colorRuleEngine.getLuckyColorsByGoal(luckyGoal);

        List<Product> allProducts = productRepository.findAllWithVariants();

        List<ScoredProduct> scored = allProducts.stream()
                .map(p -> {
                    int dayScore  = bestVariantColorScore(p, dayColors);
                    int goalScore = goalColors.isEmpty() ? 0
                                    : Math.max(0, bestVariantColorScore(p, goalColors) - 1); // 0–2 bonus
                    return new ScoredProduct(p, dayScore + goalScore);
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
        if (targetColors.isEmpty()) return 0;
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
