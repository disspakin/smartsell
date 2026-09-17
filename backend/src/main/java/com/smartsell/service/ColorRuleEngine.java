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
    // =====================================================================
    private static final Map<String, List<String>> LUCKY_COLOR_BY_DAY = Map.of(
        "อาทิตย์", List.of("Orange", "Red", "Pink", "Green", "White"),
        "จันทร์",  List.of("Green", "Black", "White", "Purple"),
        "อังคาร",  List.of("Black", "Yellow", "Pink", "Purple", "Red"),
        "พุธ",     List.of("Green", "Light Yellow", "Yellow"),
        "พฤหัส",  List.of("Orange", "Yellow", "Sky Blue", "Navy", "Green", "Red"),
        "ศุกร์",   List.of("Sky Blue", "Navy", "White", "Yellow", "Pink"),
        "เสาร์",   List.of("Red", "Yellow", "Sky Blue", "Navy", "Pink", "Brown")
    );

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
        put("Navy",        List.of("navy", "navy blue", "dark blue", "น้ำเงิน", "น้ำเงินเข้ม", "กรมท่า", "สีน้ำเงิน"));
        put("Sky Blue",    List.of("sky blue", "light blue", "ฟ้า", "สีฟ้า", "ฟ้าอ่อน", "baby blue", "powder blue"));
        put("Yellow",      List.of("yellow", "เหลือง", "สีเหลือง", "light yellow", "เหลืองอ่อน"));
        put("Gold",        List.of("gold", "ทอง", "สีทอง", "golden", "เหลืองทอง", "navy gold"));
        put("Red",         List.of("red", "แดง", "สีแดง", "crimson", "scarlet"));
        put("Pink",        List.of("pink", "ชมพู", "สีชมพู", "rose pink", "hot pink", "baby pink"));
        put("Green",       List.of("green", "เขียว", "สีเขียว", "apple green", "dark green", "olive green"));
        put("Orange",      List.of("orange", "ส้ม", "สีส้ม", "burnt orange", "coral", "peach"));
        put("Purple",      List.of("purple", "ม่วง", "สีม่วง", "violet", "lavender", "indigo"));
        put("Brown",       List.of("brown", "น้ำตาล", "สีน้ำตาล", "camel", "tan", "khaki", "rust"));
        put("Gray",        List.of("gray", "grey", "เทา", "สีเทา", "light gray", "charcoal gray"));
        put("Peach",       List.of("peach", "พีช", "สีพีช", "salmon", "apricot"));
        put("Mustard",     List.of("mustard", "มัสตาร์ด", "dark yellow", "เหลืองมัสตาร์ด"));
        put("Burgundy",    List.of("burgundy", "เบอร์กันดี", "wine", "maroon", "dark red"));
        put("Lavender",    List.of("lavender", "ลาเวนเดอร์", "light purple", "lilac"));
        put("Ivory",       List.of("ivory", "ครีม", "cream", "off-white", "beige"));
        put("Olive",       List.of("olive", "มะกอก", "olive green", "dark olive"));
        put("Coral",       List.of("coral", "คอรัล", "salmon pink"));
    }};

    // =====================================================================
    // PUBLIC API
    // =====================================================================

    /** Return recommended colors for a given Personal Color season */
    public List<String> getPersonalColorList(String season) {
        return PERSONAL_COLOR_RULES.getOrDefault(season, List.of());
    }

    /** Return auspicious colors for a given birth day (วันเกิด) */
    public List<String> getLuckyColorsByDay(String day) {
        return LUCKY_COLOR_BY_DAY.getOrDefault(day, List.of());
    }

    /** Return recommended colors for a given goal (เสริมด้านอะไร) */
    public List<String> getLuckyColorsByGoal(String goal) {
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

        for (String target : targetColors) {
            List<String> aliases = COLOR_ALIASES.getOrDefault(target, List.of());

            // Check canonical name itself
            if (vc.equals(target.toLowerCase())) return 3;

            // Check aliases (exact)
            for (String alias : aliases) {
                if (vc.equals(alias.toLowerCase())) return 3;
            }

            // Check aliases (contains / substring — looser match)
            for (String alias : aliases) {
                if (vc.contains(alias.toLowerCase()) || alias.toLowerCase().contains(vc)) return 2;
            }
        }
        return 1; // fallback
    }
}
