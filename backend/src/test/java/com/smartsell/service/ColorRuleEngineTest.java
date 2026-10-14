package com.smartsell.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ColorRuleEngine")
class ColorRuleEngineTest {

    private static final List<String> DAYS = List.of("อาทิตย์", "จันทร์", "อังคาร", "พุธ", "พฤหัส", "ศุกร์", "เสาร์");

    private final ColorRuleEngine engine = new ColorRuleEngine();

    @Test
    @DisplayName("อังคารกับศุกร์มีสีแสดในกฎสีวันเกิด")
    void tuesdayAndFridayHaveVermilion() {
        assertThat(engine.getLuckyColorsByDay("อังคาร")).contains("Vermilion");
        assertThat(engine.getLuckyColorsByDay("ศุกร์")).contains("Vermilion");
    }

    @Test
    @DisplayName("ทุกวันเกิดคู่กับทุกด้านที่เสริม (รวมไม่เลือก) ต้องมีชื่อไทยและ hex ครบ และแสดงไม่เกิน 4 สี")
    void everyRankedColorHasDisplay() {
        List<String> goals = java.util.Arrays.asList(null, "การเรียน", "การงาน", "การเงิน", "ความรัก", "โชคลาภ", "ความมั่นใจ");
        for (String day : DAYS) {
            for (String goal : goals) {
                assertThat(engine.rankLuckyColors(day, goal)).as(day + "/" + goal)
                        .hasSizeBetween(1, ColorRuleEngine.MAX_DISPLAY_COLORS)
                        .allSatisfy(d -> {
                            assertThat(d).isNotNull();
                            assertThat(d.thaiName()).isNotBlank();
                            assertThat(d.hex()).matches("#[0-9a-fA-F]{6}");
                        });
            }
        }
    }

    @Test
    @DisplayName("เรียงสี: ตรงทั้งวันเกิดและด้านที่เสริม > แค่ด้านที่เสริม > แค่วันเกิด")
    void ranksByGroupThenRulebaseOrder() {
        // จันทร์ (เขียว ดำ ขาว ม่วง) + การเงิน (เขียว ทอง เหลือง ดำ ม่วง)
        // ตรงทั้งคู่: เขียว ดำ ม่วง (ลำดับวันเกิด) → แค่การเงิน: ทอง เหลือง → แค่วันเกิด: ขาว — ตัดเหลือ 4
        assertThat(names("จันทร์", "การเงิน")).containsExactly("เขียว", "ดำ", "ม่วง", "ทอง");
        // จันทร์ + การเรียน (เหลือง ส้ม เขียว ฟ้า): ตรงทั้งคู่แค่เขียว ตามด้วยสีการเรียนตามลำดับ
        assertThat(names("จันทร์", "การเรียน")).containsExactly("เขียว", "เหลือง", "ส้ม", "ฟ้า");
    }

    @Test
    @DisplayName("ถ้าสีด้านที่เสริมเต็ม 4 ช่อง ช่องสุดท้ายต้องเป็นสีวันเกิดอันดับแรก")
    void reservesOneSlotForBirthDayColor() {
        // จันทร์ + ความรัก (ชมพู แดง ส้ม พีช) ไม่มีสีซ้ำกันเลย
        assertThat(names("จันทร์", "ความรัก")).containsExactly("ชมพู", "แดง", "ส้ม", "เขียว");
    }

    @Test
    @DisplayName("ไม่เลือกด้านที่เสริม ได้สีวันเกิดตามลำดับเดิม ไม่เกิน 4 สี")
    void noGoalUsesBirthDayColorsOnly() {
        assertThat(names("อังคาร", null)).containsExactly("ดำ", "เหลือง", "ชมพู", "ม่วง");
        assertThat(names("พุธ", null)).containsExactly("เขียว", "เหลืองอ่อน", "เหลืองแก่");
    }

    private List<String> names(String day, String goal) {
        return engine.rankLuckyColors(day, goal).stream().map(ColorRuleEngine.ColorDisplay::thaiName).toList();
    }

    @Test
    @DisplayName("ชื่อสีไทยในสินค้าจริงต้องจับคู่ได้")
    void matchesThaiProductColorNames() {
        assertThat(engine.scoreColor("บานเย็น", List.of("Pink"))).isEqualTo(3);
        assertThat(engine.scoreColor("บานเย็น-ครีม", List.of("Pink"))).isEqualTo(2);
        assertThat(engine.scoreColor("แสด", List.of("Vermilion"))).isEqualTo(3);
        assertThat(engine.scoreColor("มัลเบอรี่-เหลือง", List.of("Burgundy"))).isEqualTo(2);
        assertThat(engine.scoreColor("แดงเลือดหมู-ครีม", List.of("Burgundy"))).isEqualTo(2);
        assertThat(engine.scoreColor("เขียวขี้ม้า-แดง", List.of("Olive"))).isEqualTo(2);
        assertThat(engine.scoreColor("กรม", List.of("Navy"))).isEqualTo(3);
        assertThat(engine.scoreColor("วอลนัท-เหลือง", List.of("Brown"))).isEqualTo(2);
    }

    @Test
    @DisplayName("ตรงเป๊ะต้องชนะ แม้สีที่ตรงบางส่วนจะอยู่ก่อนในรายการ")
    void exactMatchWinsOverEarlierPartialMatch() {
        // Gold มี alias "เหลืองทอง" ที่มีคำว่าเหลืองซ้อนอยู่ และอยู่ก่อน Yellow ในรายการการเงิน
        assertThat(engine.scoreColor("เหลือง", List.of("Green", "Gold", "Yellow", "Black", "Purple"))).isEqualTo(3);
    }

    @Test
    @DisplayName("วันและเป้าหมายที่ไม่มีจริงต้องไม่ผ่านการตรวจ")
    void rejectsUnknownDayAndGoal() {
        assertThat(engine.isKnownDay("จันทร์")).isTrue();
        assertThat(engine.isKnownDay("วันหยุด")).isFalse();
        assertThat(engine.isKnownDay(null)).isFalse();
        assertThat(engine.isKnownGoal("ความรัก")).isTrue();
        assertThat(engine.isKnownGoal("อะไรก็ได้")).isFalse();
    }
}
