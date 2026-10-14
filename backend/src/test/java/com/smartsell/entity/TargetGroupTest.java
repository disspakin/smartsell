package com.smartsell.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TargetGroup")
class TargetGroupTest {

    @Test
    @DisplayName("ผู้ชายได้เสื้อผู้ชายกับ unisex / ผู้หญิงได้เสื้อผู้หญิงกับ unisex")
    void matchesGender() {
        assertThat(TargetGroup.suitsAdult("MALE", "MALE")).isTrue();
        assertThat(TargetGroup.suitsAdult("UNISEX", "MALE")).isTrue();
        assertThat(TargetGroup.suitsAdult("FEMALE", "MALE")).isFalse();

        assertThat(TargetGroup.suitsAdult("FEMALE", "FEMALE")).isTrue();
        assertThat(TargetGroup.suitsAdult("UNISEX", "FEMALE")).isTrue();
        assertThat(TargetGroup.suitsAdult("MALE", "FEMALE")).isFalse();
    }

    @Test
    @DisplayName("ไม่ระบุเพศได้ทุกตัว ยกเว้นเสื้อเด็กที่ไม่แนะนำเลย")
    void anyGenderGetsAllButKids() {
        for (String gender : new String[] {"ANY", null}) {
            assertThat(TargetGroup.suitsAdult("MALE", gender)).isTrue();
            assertThat(TargetGroup.suitsAdult("FEMALE", gender)).isTrue();
            assertThat(TargetGroup.suitsAdult("UNISEX", gender)).isTrue();
            assertThat(TargetGroup.suitsAdult("KIDS", gender)).isFalse();
        }
        assertThat(TargetGroup.suitsAdult("KIDS", "MALE")).isFalse();
    }

    @Test
    @DisplayName("รับเฉพาะ MALE / FEMALE / ANY จาก AI ค่าอื่นถือว่ายังไม่ได้ตอบ")
    void normalizesCustomerGender() {
        assertThat(TargetGroup.normalizeCustomerGender(" female ")).isEqualTo("FEMALE");
        assertThat(TargetGroup.normalizeCustomerGender("ANY")).isEqualTo("ANY");
        assertThat(TargetGroup.normalizeCustomerGender("UNISEX")).isNull();
        assertThat(TargetGroup.normalizeCustomerGender("ชาย")).isNull();
        assertThat(TargetGroup.normalizeCustomerGender(null)).isNull();
    }
}
