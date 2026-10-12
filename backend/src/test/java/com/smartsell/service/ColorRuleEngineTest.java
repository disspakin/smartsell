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
    @DisplayName("ทุกสีมงคลของทุกวันต้องมีชื่อไทยและ hex ให้แสดง")
    void everyLuckyColorHasDisplay() {
        for (String day : DAYS) {
            List<ColorRuleEngine.ColorDisplay> display = engine.getLuckyColorDisplayByDay(day);
            assertThat(display).as(day).hasSameSizeAs(engine.getLuckyColorsByDay(day));
            assertThat(display).as(day).allSatisfy(d -> {
                assertThat(d).isNotNull();
                assertThat(d.thaiName()).isNotBlank();
                assertThat(d.hex()).matches("#[0-9a-fA-F]{6}");
            });
        }
    }

    @Test
    @DisplayName("สีที่แสดงต้องตรงกับสีที่ใช้คัดสินค้า — อังคารกับศุกร์มีแสดทั้งสองที่")
    void displayMatchesScoringColors() {
        assertThat(engine.getLuckyColorDisplayByDay("อังคาร")).extracting(ColorRuleEngine.ColorDisplay::thaiName)
                .containsExactly("ดำ", "เหลือง", "ชมพู", "ม่วง", "แดง", "แสด");
        assertThat(engine.getLuckyColorsByDay("อังคาร")).contains("Vermilion");
        assertThat(engine.getLuckyColorsByDay("ศุกร์")).contains("Vermilion");
        assertThat(engine.getLuckyColorDisplayByDay("พุธ")).extracting(ColorRuleEngine.ColorDisplay::thaiName)
                .containsExactly("เขียว", "เหลืองอ่อน", "เหลืองแก่");
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
    @DisplayName("วันและเป้าหมายที่ไม่มีจริงต้องไม่ผ่านการตรวจ")
    void rejectsUnknownDayAndGoal() {
        assertThat(engine.isKnownDay("จันทร์")).isTrue();
        assertThat(engine.isKnownDay("วันหยุด")).isFalse();
        assertThat(engine.isKnownDay(null)).isFalse();
        assertThat(engine.isKnownGoal("ความรัก")).isTrue();
        assertThat(engine.isKnownGoal("อะไรก็ได้")).isFalse();
    }
}
