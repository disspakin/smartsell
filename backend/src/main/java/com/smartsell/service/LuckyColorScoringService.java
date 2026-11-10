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
import java.util.stream.Stream;

/**
 * Scoring engine for Lucky Color recommendations (independent from Personal Color).
 *
 * แนะนำเป็น "สินค้า + สี": หนึ่งการ์ดคือสินค้าหนึ่งตัวในสีหนึ่งสี
 * สินค้าที่มีหลายสีตรง (เช่น เสื้อสีพื้นมีทั้งเขียวมะนาวและส้ม) จึงขึ้นได้หลายการ์ด คนละสี
 *
 * คัดเฉพาะสีที่ตรงเป๊ะ (ชื่อหรือชื่อเรียก) กับวงกลมสีที่ลูกค้าเห็น (ColorRuleEngine.rankLuckyColorNames)
 * เสื้อสองสีเทียบด้วยสีหลัก (สีแรก) เช่น "ดำ-ครีม" นับเป็นดำ แต่ "เทา-ส้ม" ไม่นับเป็นส้ม
 * สีวันเกิดที่ตกรอบวงกลมไม่ถูกแนะนำ — ลูกค้าเห็นวงกลมสีไหน การ์ดก็ต้องเป็นสีนั้น
 *
 * เรียงตามคะแนนกลุ่ม (ColorRuleEngine.luckyGroupScore): ตรงทั้งวันเกิดและเป้าหมาย = 3, แค่เป้าหมาย = 2, แค่วันเกิด = 1
 * คะแนนเท่ากันเรียงตาม id สินค้า แล้วตาม id ของสี ให้ผลเหมือนเดิมทุกครั้ง
 * มีไม่ถึง topN ก็แสดงเท่าที่มี ไม่เอาสีที่ไม่ตรงมาเติม
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
     * @param luckyGoal    เสริมด้านไหน (การเรียน, การงาน, ...) หรือ null = ไม่เลือก
     * @param gender       MALE / FEMALE / ANY หรือ null — กรองแบบเดียวกับผู้ช่วย AI และไม่แนะนำเสื้อเด็ก
     * @param topN         จำนวนการ์ดสูงสุดที่ return
     */
    public List<LuckyColorDTO.LuckyProduct> recommend(String birthWeekday, String luckyGoal, String gender, int topN) {
        List<String> shown = colorRuleEngine.rankLuckyColorNames(birthWeekday, luckyGoal);
        // แยกสีในวงกลมเป็นสีวันเกิด / สีด้านที่เสริม — สีเดียวอยู่ได้ทั้งสองชุด
        List<String> shownDay = shown.stream().filter(colorRuleEngine.getLuckyColorsByDay(birthWeekday)::contains).toList();
        List<String> shownGoal = shown.stream().filter(colorRuleEngine.getLuckyColorsByGoal(luckyGoal)::contains).toList();

        return productRepository.findAllWithVariants().stream()
                .filter(p -> TargetGroup.suitsAdult(p.getTargetGroup(), gender))
                .flatMap(p -> colorPicksOf(p, shownDay, shownGoal))
                .sorted(Comparator.comparingInt(ColorPick::score).reversed()
                        .thenComparing(pick -> pick.product().getId())
                        .thenComparing(pick -> pick.variant().getId(), Comparator.nullsLast(Comparator.naturalOrder())))
                .limit(topN)
                .map(this::toDTO)
                .toList();
    }

    // ── helpers ──────────────────────────────────────────────────────────

    /** ทุกสีของสินค้านี้ที่ตรงเป๊ะกับวงกลมสี สีละหนึ่งการ์ด */
    Stream<ColorPick> colorPicksOf(Product p, List<String> shownDay, List<String> shownGoal) {
        // หนึ่งสีมีหลายไซส์ เก็บ variant ตัวแทนของแต่ละสี: เลือกตัวที่มีรูปก่อน ถ้าเท่ากันเลือก id น้อยสุด
        // (ลำดับ variants จาก DB ไม่แน่นอน ห้ามพึ่ง)
        Map<String, ProductVariant> byColor = new LinkedHashMap<>();
        p.getVariants().stream()
                .filter(v -> v.getColor() != null && !v.getColor().isBlank())
                .sorted(Comparator.comparing(ProductVariant::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                .forEach(v -> byColor.merge(v.getColor(), v,
                        (kept, other) -> kept.getImageUrl() == null && other.getImageUrl() != null ? other : kept));

        return byColor.values().stream()
                .map(v -> new ColorPick(p, v, colorScore(v.getColor(), shownDay, shownGoal)))
                .filter(pick -> pick.score() > 0);
    }

    /** คะแนนกลุ่มของสีนี้ — 0 ถ้าไม่ตรงเป๊ะกับสีไหนในวงกลมเลย */
    int colorScore(String color, List<String> shownDay, List<String> shownGoal) {
        return ColorRuleEngine.luckyGroupScore(isExact(color, shownDay), isExact(color, shownGoal));
    }

    private boolean isExact(String color, List<String> targets) {
        return !targets.isEmpty() && colorRuleEngine.scoreColor(primaryColor(color), targets) == 3;
    }

    /**
     * สีหลักของเสื้อ — เสื้อสองสีเขียนสีหลักไว้หน้า "-" เสมอ เช่น "ดำ-ครีม" = ดำ, "เทา-ส้ม" = เทา
     * ใช้แค่ตอนเทียบกับวงกลมสี การ์ดยังแสดงชื่อสีเต็ม
     */
    static String primaryColor(String color) {
        int dash = color.indexOf('-');
        return dash > 0 ? color.substring(0, dash).trim() : color.trim();
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

    record ColorPick(Product product, ProductVariant variant, int score) {}
}
