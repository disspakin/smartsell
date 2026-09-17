package com.smartsell.service;

import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Occasion → product category scoring rules.
 * Only Polo and T-Shirt categories are in the catalog.
 * Modify only this file to update occasion rules.
 */
@Component
public class OccasionRuleEngine {

    // occasion → (category → score)
    private static final Map<String, Map<String, Integer>> OCCASION_SCORES = Map.of(
        "ทางการ", Map.of(
            "Polo",   3,
            "T-Shirt", 1
        ),
        "ทั่วไป", Map.of(
            "T-Shirt", 3,
            "Polo",    2
        ),
        "เข้ากิจกรรม", Map.of(
            "T-Shirt", 3,
            "Polo",    3
        )
    );

    /**
     * Return the score for a product category given the user's occasion.
     * Returns 0 if occasion or category is unknown.
     */
    public int score(String occasion, String category) {
        if (occasion == null || category == null) return 0;
        Map<String, Integer> categoryMap = OCCASION_SCORES.get(occasion);
        if (categoryMap == null) return 0;
        return categoryMap.getOrDefault(category, 0);
    }
}
