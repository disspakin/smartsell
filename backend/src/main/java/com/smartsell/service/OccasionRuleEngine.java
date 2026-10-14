package com.smartsell.service;

import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Occasion → product category scoring rules.
 * หมวดหมู่สินค้ามี 3 หมวด: โปโล / เสื้อยืด / เสื้อครอป (ดู database/normalize_categories.sql)
 * เป็นคะแนน ไม่ใช่ตัวกรอง — 0 แปลว่าไม่เหมาะ แต่ยังถูกแนะนำได้ถ้าสินค้าที่เหมาะมีไม่พอ
 * Modify only this file to update occasion rules.
 */
@Component
public class OccasionRuleEngine {

    // occasion → (category → score)
    private static final Map<String, Map<String, Integer>> OCCASION_SCORES = Map.of(
        "ทางการ", Map.of(
            "โปโล",     3,
            "เสื้อยืด",   0,
            "เสื้อครอป",  0
        ),
        "ทั่วไป", Map.of(
            "โปโล",     2,
            "เสื้อยืด",   3,
            "เสื้อครอป",  3
        ),
        "เข้ากิจกรรม", Map.of(
            "โปโล",     2,
            "เสื้อยืด",   3,
            "เสื้อครอป",  1
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
