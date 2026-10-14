package com.smartsell.service;

import com.smartsell.dto.LuckyColorDTO;
import com.smartsell.entity.Product;
import com.smartsell.entity.ProductVariant;
import com.smartsell.entity.TargetGroup;
import com.smartsell.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Scoring engine for Lucky Color recommendations (independent from Personal Color).
 *
 * แนะนำเป็น "สินค้า + สี" ไม่ใช่แค่สินค้า: ให้คะแนนทีละสีของทุกสินค้า แล้วเอาสีที่ดีที่สุดของแต่ละสินค้า
 * สินค้าหลายสี (โปโล, เสื้อสีพื้น 16 สี) จึงแนะนำได้ทุกวันเกิดในสีที่ตรงกับวันนั้น
 * ลูกค้าซื้อเสื้อตัวเดียวสีเดียว สีอื่นที่สินค้ามีจึงไม่ควรมาลดหรือเพิ่มคะแนน
 *
 * คะแนนของหนึ่งสี = คะแนนกลุ่ม × ความตรง (เป้าหมายไม่บังคับ)
 *   คะแนนกลุ่ม (ColorRuleEngine.luckyGroupScore): ตรงทั้งวันเกิดและเป้าหมาย = 3, แค่เป้าหมาย = 2, แค่วันเกิด = 1
 *   ความตรง: ตรงเป๊ะ/ชื่อเรียกตรง = 1.0, ตรงบางส่วน (เช่น "ดำ-ครีม" กับ ดำ) = 0.6
 *     ถ้าตรงทั้งสองชุด ใช้ค่าที่มากกว่า — ตรงเป๊ะในชุดใดชุดหนึ่งถือว่าตรงเป๊ะ
 * ใช้คะแนนกลุ่มเดียวกับวงกลมสีที่แสดง สินค้าจึงเรียงไปทางเดียวกับสีที่ลูกค้าเห็น
 * หนึ่งสินค้าขึ้นได้การ์ดเดียว ไม่ให้โปโลกินหลายช่อง และสินค้าที่ไม่มีสีตรงเลย (คะแนน 0) ไม่ถูกแนะนำ
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
     * @param birthWeekday วันเกิด (จันทร์, อังคาร, ...)
     * @param luckyGoal    เสริมด้านไหน (การเรียน, การงาน, ...) หรือ null
     * @param gender       MALE / FEMALE / ANY หรือ null — กรองแบบเดียวกับผู้ช่วย AI และไม่แนะนำเสื้อเด็ก
     * @param topN         จำนวนสินค้าที่ return
     */
    public List<LuckyColorDTO.LuckyProduct> recommend(String birthWeekday, String luckyGoal, String gender, int topN) {
        List<String> dayColors  = colorRuleEngine.getLuckyColorsByDay(birthWeekday);
        List<String> goalColors = colorRuleEngine.getLuckyColorsByGoal(luckyGoal);

        return productRepository.findAllWithVariants().stream()
                .filter(p -> TargetGroup.suitsAdult(p.getTargetGroup(), gender))
                .map(p -> bestColorOf(p, dayColors, goalColors))
                .filter(Objects::nonNull)
                // แสดงเฉพาะสินค้าที่มีสีตรงวันเกิดหรือด้านที่เสริม ไม่เอาสีที่ไม่เกี่ยวมาเติมให้ครบจำนวน
                .filter(pick -> pick.score() > 0)
                // คะแนนเท่ากันเรียงตาม id ให้ผลเหมือนเดิมทุกครั้งที่ถาม
                .sorted(Comparator.comparingDouble(ColorPick::score).reversed()
                        .thenComparing(pick -> pick.product().getId()))
                .limit(topN)
                .map(this::toDTO)
                .toList();
    }

    // ── helpers ──────────────────────────────────────────────────────────

    /** สีที่ได้คะแนนสูงสุดของสินค้านี้ — null ถ้าสินค้าไม่มี variant ที่ระบุสี */
    ColorPick bestColorOf(Product p, List<String> dayColors, List<String> goalColors) {
        // หนึ่งสีมีหลายไซส์ เก็บ variant ตัวแทนของแต่ละสี โดยเลือกตัวที่มีรูปก่อน
        Map<String, ProductVariant> byColor = new LinkedHashMap<>();
        for (ProductVariant v : p.getVariants()) {
            if (v.getColor() == null || v.getColor().isBlank()) continue;
            byColor.merge(v.getColor(), v, (kept, other) -> kept.getImageUrl() == null ? other : kept);
        }

        return byColor.values().stream()
                .map(v -> new ColorPick(p, v, colorScore(v.getColor(), dayColors, goalColors)))
                // คะแนนเท่ากันเลือก variant id น้อยสุด — ลำดับ variants จาก DB ไม่แน่นอน ห้ามพึ่ง
                .max(Comparator.comparingDouble(ColorPick::score)
                        .thenComparing(pick -> pick.variant().getId(), Comparator.nullsFirst(Comparator.reverseOrder())))
                .orElse(null);
    }

    double colorScore(String color, List<String> dayColors, List<String> goalColors) {
        double day = colorMatch(color, dayColors);
        double goal = colorMatch(color, goalColors);
        int group = ColorRuleEngine.luckyGroupScore(day > 0, goal > 0);
        if (group == 0) return 0;
        return group * Math.max(day, goal);
    }

    private double colorMatch(String color, List<String> targetColors) {
        if (targetColors.isEmpty()) return 0;
        return switch (colorRuleEngine.scoreColor(color, targetColors)) {
            case 3 -> 1.0;
            case 2 -> 0.6;
            default -> 0.0;
        };
    }

    private LuckyColorDTO.LuckyProduct toDTO(ColorPick pick) {
        Product p = pick.product();
        ProductVariant v = pick.variant();
        // สีนี้ไม่มีรูปของตัวเอง ใช้รูปของสินค้า หรือรูปของ variant แรกที่มีรูป (แบบเดียวกับ ProductDTO)
        String image = v.getImageUrl() != null ? v.getImageUrl()
                : p.getImageUrl() != null ? p.getImageUrl()
                : p.getVariants().stream().map(ProductVariant::getImageUrl).filter(Objects::nonNull).findFirst().orElse(null);
        return new LuckyColorDTO.LuckyProduct(p.getId(), p.getName(), p.getPrice(), image,
                v.getColor(), v.getColorHex());
    }

    record ColorPick(Product product, ProductVariant variant, double score) {}
}
