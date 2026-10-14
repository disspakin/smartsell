package com.smartsell.service;

import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Central rulebase for color-based recommendations.
 * Modify only this file to update any color rules — no DB changes needed.
 */
@Component
public class ColorRuleEngine {

    // =====================================================================
    // PERSONAL COLOR RULES
    // season → recommended color names (canonical English names)
    // =====================================================================
    private static final Map<String, List<String>> PERSONAL_COLOR_RULES = Map.of(
        "Spring",  List.of("Peach", "Coral", "Orange", "Yellow", "Cream", "Ivory", "Apple Green", "Sky Blue"),
        "Summer",  List.of("Rose Pink", "Pink", "Sky Blue", "Lavender", "Light Gray", "Powder Blue", "Soft White"),
        "Autumn",  List.of("Rust", "Brown", "Mustard", "Olive", "Camel", "Dark Green", "Gold", "Burnt Orange"),
        "Winter",  List.of("Black", "White", "Navy", "Red", "Burgundy", "Royal Blue", "Magenta", "Dark Blue")
    );

    // =====================================================================
    // LUCKY COLOR RULES
    // day (วันเกิด) → list of auspicious colors (สีมงคล)
    // Source: ความเชื่อไทยดั้งเดิมเรื่องสีประจำวันเกิด
    //
    // ที่นี่คือที่เดียวที่กำหนดสีมงคล — ทั้งสีที่แสดงให้ลูกค้าเห็นและสีที่ใช้คัดสินค้า
    // อ่านจาก list นี้ทั้งคู่ (ชื่อไทย/hex มาจาก LUCKY_COLOR_DISPLAY) จึงไม่มีทางไม่ตรงกัน
    // =====================================================================
    private static final Map<String, List<String>> LUCKY_COLOR_BY_DAY = Map.of(
        "อาทิตย์", List.of("Orange", "Red", "Pink", "Green", "White"),
        "จันทร์",  List.of("Green", "Black", "White", "Purple"),
        "อังคาร",  List.of("Black", "Yellow", "Pink", "Purple", "Red", "Vermilion"),
        "พุธ",     List.of("Green", "Light Yellow", "Mustard"),
        "พฤหัส",  List.of("Orange", "Yellow", "Sky Blue", "Navy", "Green", "Red"),
        "ศุกร์",   List.of("Sky Blue", "Navy", "White", "Yellow", "Pink", "Vermilion"),
        "เสาร์",   List.of("Red", "Yellow", "Sky Blue", "Navy", "Pink", "Brown")
    );

    /** ชื่อไทยและ hex สำหรับแสดงสีมงคลบนหน้าเว็บ — ทุกสีใน LUCKY_COLOR_BY_DAY ต้องมีอยู่ที่นี่ */
    private static final Map<String, ColorDisplay> LUCKY_COLOR_DISPLAY = Map.ofEntries(
        Map.entry("Orange",       new ColorDisplay("ส้ม", "#e07a3d")),
        Map.entry("Red",          new ColorDisplay("แดง", "#c94f4f")),
        Map.entry("Pink",         new ColorDisplay("ชมพู", "#e88fa8")),
        Map.entry("Green",        new ColorDisplay("เขียว", "#6fa87f")),
        Map.entry("White",        new ColorDisplay("ขาว", "#FFFFFF")),
        Map.entry("Black",        new ColorDisplay("ดำ", "#1a1a1a")),
        Map.entry("Purple",       new ColorDisplay("ม่วง", "#8f6fa8")),
        Map.entry("Yellow",       new ColorDisplay("เหลือง", "#f4d35e")),
        Map.entry("Vermilion",    new ColorDisplay("แสด", "#e05a20")),
        Map.entry("Light Yellow", new ColorDisplay("เหลืองอ่อน", "#f0e68c")),
        Map.entry("Mustard",      new ColorDisplay("เหลืองแก่", "#c9a800")),
        Map.entry("Sky Blue",     new ColorDisplay("ฟ้า", "#6fa8c9")),
        Map.entry("Navy",         new ColorDisplay("น้ำเงิน", "#1E3A8A")),
        Map.entry("Brown",        new ColorDisplay("น้ำตาล", "#a0693a")),
        // สีที่มีแค่ใน LUCKY_COLOR_BY_GOAL — แสดงด้วยเมื่อลูกค้าเลือกด้านที่อยากเสริม
        Map.entry("Gray",         new ColorDisplay("เทา", "#95a5a6")),
        Map.entry("Gold",         new ColorDisplay("ทอง", "#d4af37")),
        Map.entry("Peach",        new ColorDisplay("พีช", "#f6b48f"))
    );

    // =====================================================================
    // LUCKY COLOR RANKING
    // คะแนนตามกลุ่ม: ตรงทั้งวันเกิดและด้านที่เสริม > ตรงแค่ด้านที่เสริม > ตรงแค่วันเกิด
    // ใช้ทั้งเรียงวงกลมสีที่แสดง (rankLuckyColors) และคัดสินค้า (LuckyColorScoringService)
    // =====================================================================
    public static final int SCORE_DAY_AND_GOAL = 3;
    public static final int SCORE_GOAL_ONLY = 2;
    public static final int SCORE_DAY_ONLY = 1;

    /** จำนวนสีมงคลที่แสดงบนหน้าเว็บ */
    public static final int MAX_DISPLAY_COLORS = 4;

    /** คะแนนกลุ่มจากว่าสีนั้นอยู่ในสีวันเกิด / สีด้านที่เสริมหรือไม่ — 0 ถ้าไม่อยู่ในทั้งสองชุด */
    public static int luckyGroupScore(boolean inDay, boolean inGoal) {
        if (inDay && inGoal) return SCORE_DAY_AND_GOAL;
        if (inGoal) return SCORE_GOAL_ONLY;
        if (inDay) return SCORE_DAY_ONLY;
        return 0;
    }

    public record ColorDisplay(String thaiName, String hex) {}

    // =====================================================================
    // GOAL-BASED LUCKY COLOR RULES
    // goal → recommended colors (สีเสริมดวงตามด้านที่ต้องการ)
    // Sources: ความเชื่อสีมงคลเรื่องการเสริมดวงด้านต่างๆ
    // =====================================================================
    private static final Map<String, List<String>> LUCKY_COLOR_BY_GOAL = Map.of(
        "การเรียน",   List.of("Yellow", "Orange", "Green", "Sky Blue"),   // เสริมสมาธิ ความจำ
        "การงาน",     List.of("Sky Blue", "Navy", "Gray", "Orange", "Brown"), // น่าเชื่อถือ ราบรื่น
        "การเงิน",    List.of("Green", "Gold", "Yellow", "Black", "Purple"),  // ดึงดูดทรัพย์
        "ความรัก",    List.of("Pink", "Red", "Orange", "Peach"),               // เสน่ห์ เมตตา
        "โชคลาภ",    List.of("Green", "Gold", "Yellow", "Purple", "Black"),   // โชคลาภโดยรวม
        "ความมั่นใจ", List.of("Red", "Black", "Purple", "Navy")                // บารมี อำนาจ
    );

    // =====================================================================
    // COLOR ALIAS MAP
    // canonical name → list of aliases (case-insensitive matching)
    // =====================================================================
    private static final Map<String, List<String>> COLOR_ALIASES = new LinkedHashMap<>() {{
        put("Black",       List.of("black", "ดำ", "สีดำ", "jet black", "charcoal"));
        put("White",       List.of("white", "ขาว", "สีขาว", "cream white", "off white", "ivory white"));
        put("Navy",        List.of("navy", "navy blue", "dark blue", "น้ำเงิน", "น้ำเงินเข้ม", "กรม", "กรมท่า", "สีน้ำเงิน", "ยีนส์", "denim"));
        put("Sky Blue",    List.of("sky blue", "light blue", "ฟ้า", "สีฟ้า", "ฟ้าอ่อน", "baby blue", "powder blue"));
        put("Yellow",      List.of("yellow", "เหลือง", "สีเหลือง", "light yellow", "เหลืองอ่อน"));
        put("Light Yellow", List.of("light yellow", "เหลืองอ่อน", "สีเหลืองอ่อน", "pale yellow"));
        put("Gold",        List.of("gold", "ทอง", "สีทอง", "golden", "เหลืองทอง", "navy gold"));
        put("Red",         List.of("red", "แดง", "สีแดง", "crimson", "scarlet"));
        put("Pink",        List.of("pink", "ชมพู", "สีชมพู", "rose pink", "hot pink", "baby pink", "บานเย็น"));
        put("Magenta",     List.of("magenta", "fuchsia", "บานเย็น", "สีบานเย็น"));
        put("Green",       List.of("green", "เขียว", "สีเขียว", "apple green", "dark green", "olive green", "เขียวมะนาว", "lime"));
        put("Orange",      List.of("orange", "ส้ม", "สีส้ม", "burnt orange", "coral", "peach"));
        put("Vermilion",   List.of("vermilion", "แสด", "สีแสด"));
        put("Purple",      List.of("purple", "ม่วง", "สีม่วง", "violet", "lavender", "indigo"));
        put("Brown",       List.of("brown", "น้ำตาล", "สีน้ำตาล", "camel", "tan", "khaki", "rust", "วอลนัท", "walnut"));
        put("Gray",        List.of("gray", "grey", "เทา", "สีเทา", "light gray", "charcoal gray"));
        put("Peach",       List.of("peach", "พีช", "สีพีช", "salmon", "apricot"));
        put("Mustard",     List.of("mustard", "มัสตาร์ด", "dark yellow", "เหลืองมัสตาร์ด", "เหลืองแก่"));
        put("Burgundy",    List.of("burgundy", "เบอร์กันดี", "wine", "maroon", "dark red", "เลือดหมู", "แดงเลือดหมู", "mulberry", "มัลเบอรี่"));
        put("Lavender",    List.of("lavender", "ลาเวนเดอร์", "light purple", "lilac"));
        put("Ivory",       List.of("ivory", "ครีม", "cream", "off-white", "beige", "เบจ"));
        put("Olive",       List.of("olive", "มะกอก", "olive green", "dark olive", "ขี้ม้า", "เขียวขี้ม้า"));
        put("Coral",       List.of("coral", "คอรัล", "salmon pink", "โอรส"));
    }};

    // =====================================================================
    // PUBLIC API
    // =====================================================================

    /** Return recommended colors for a given Personal Color season */
    public List<String> getPersonalColorList(String season) {
        if (season == null) {
            return List.of();
        }
        return PERSONAL_COLOR_RULES.getOrDefault(season, List.of());
    }

    /** Return auspicious colors for a given birth day (วันเกิด) */
    public List<String> getLuckyColorsByDay(String day) {
        if (day == null) {
            return List.of();
        }
        return LUCKY_COLOR_BY_DAY.getOrDefault(day, List.of());
    }

    public boolean isKnownDay(String day) {
        return day != null && LUCKY_COLOR_BY_DAY.containsKey(day);
    }

    public boolean isKnownGoal(String goal) {
        return goal != null && LUCKY_COLOR_BY_GOAL.containsKey(goal);
    }

    /**
     * สีมงคลที่จะแสดง เรียงตามคะแนนกลุ่ม (ดู luckyGroupScore) ไม่เกิน MAX_DISPLAY_COLORS สี
     *
     * คะแนนเท่ากันเรียงตามลำดับใน rulebase: กลุ่มที่มีสีวันเกิดตามลำดับ LUCKY_COLOR_BY_DAY
     * กลุ่มที่ตรงแค่ด้านที่เสริมตามลำดับ LUCKY_COLOR_BY_GOAL
     *
     * จองไว้อย่างน้อย 1 สีวันเกิดเสมอ — ถ้าสีด้านที่เสริมเต็ม 4 ช่องโดยไม่มีสีวันเกิดเลย
     * (เช่น จันทร์ + ความรัก) ช่องสุดท้ายจะเป็นสีวันเกิดอันดับแรกแทน
     * ไม่เลือกด้านที่เสริม (goal = null) ได้สีวันเกิดตามลำดับเดิม
     */
    public List<ColorDisplay> rankLuckyColors(String day, String goal) {
        List<String> dayColors = getLuckyColorsByDay(day);
        List<String> goalColors = getLuckyColorsByGoal(goal);

        // สีวันเกิดก่อน แล้วต่อด้วยสีที่มีแค่ในด้านที่เสริม — sort ที่ stable จะรักษาลำดับนี้ไว้ในคะแนนเท่ากัน
        List<String> candidates = new ArrayList<>(dayColors);
        goalColors.stream().filter(c -> !dayColors.contains(c)).forEach(candidates::add);

        List<String> ranked = candidates.stream()
                .sorted(Comparator.comparingInt((String c) ->
                        luckyGroupScore(dayColors.contains(c), goalColors.contains(c))).reversed())
                .limit(MAX_DISPLAY_COLORS)
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));

        if (!dayColors.isEmpty() && ranked.stream().noneMatch(dayColors::contains)) {
            ranked.set(ranked.size() - 1, dayColors.get(0));
        }
        return ranked.stream().map(LUCKY_COLOR_DISPLAY::get).toList();
    }

    /** Return recommended colors for a given goal (เสริมด้านอะไร) */
    public List<String> getLuckyColorsByGoal(String goal) {
        if (goal == null) {
            return List.of();
        }
        return LUCKY_COLOR_BY_GOAL.getOrDefault(goal, List.of());
    }

    /**
     * Score a product variant color against a list of target canonical colors.
     * Returns:
     *   3 = exact or direct alias match
     *   2 = partial / substring alias match
     *   1 = fallback (no match — always return at least 1 so products still appear)
     */
    public int scoreColor(String variantColor, List<String> targetColors) {
        if (variantColor == null || targetColors.isEmpty()) return 1;
        String vc = variantColor.toLowerCase().trim();

        // ไล่หาตรงเป๊ะให้ครบทุกสีก่อน แล้วค่อยหาตรงบางส่วน — ถ้าทำในรอบเดียว สีที่อยู่ก่อนในรายการ
        // จะตอบ "ตรงบางส่วน" ไปก่อนเจอสีที่ตรงเป๊ะ เช่น "เหลือง" ชน alias "เหลืองทอง" ของ Gold ก่อนถึง Yellow
        for (String target : targetColors) {
            if (vc.equals(target.toLowerCase())) return 3;
            for (String alias : COLOR_ALIASES.getOrDefault(target, List.of())) {
                if (vc.equals(alias.toLowerCase())) return 3;
            }
        }
        for (String target : targetColors) {
            for (String alias : COLOR_ALIASES.getOrDefault(target, List.of())) {
                if (vc.contains(alias.toLowerCase()) || alias.toLowerCase().contains(vc)) return 2;
            }
        }
        return 1; // fallback
    }
}
