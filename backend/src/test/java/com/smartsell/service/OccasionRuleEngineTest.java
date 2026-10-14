package com.smartsell.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("OccasionRuleEngine")
class OccasionRuleEngineTest {

    private final OccasionRuleEngine engine = new OccasionRuleEngine();

    @Test
    @DisplayName("ให้คะแนนตามหมวดหมู่ภาษาไทยที่ใช้ใน DB จริง")
    void scoresThaiCategories() {
        assertThat(engine.score("ทางการ", "โปโล")).isEqualTo(3);
        assertThat(engine.score("ทางการ", "เสื้อยืด")).isZero();
        assertThat(engine.score("ทางการ", "เสื้อครอป")).isZero();

        assertThat(engine.score("ทั่วไป", "โปโล")).isEqualTo(2);
        assertThat(engine.score("ทั่วไป", "เสื้อยืด")).isEqualTo(3);
        assertThat(engine.score("ทั่วไป", "เสื้อครอป")).isEqualTo(3);

        assertThat(engine.score("เข้ากิจกรรม", "โปโล")).isEqualTo(2);
        assertThat(engine.score("เข้ากิจกรรม", "เสื้อยืด")).isEqualTo(3);
        assertThat(engine.score("เข้ากิจกรรม", "เสื้อครอป")).isEqualTo(1);
    }

    @Test
    @DisplayName("โอกาสหรือหมวดหมู่ที่ไม่รู้จักได้ 0")
    void unknownGetsZero() {
        assertThat(engine.score("ใส่ไปเรียน", "โปโล")).isZero();
        assertThat(engine.score("ทางการ", "Polo")).isZero();
        assertThat(engine.score(null, "โปโล")).isZero();
        assertThat(engine.score("ทางการ", null)).isZero();
    }
}
