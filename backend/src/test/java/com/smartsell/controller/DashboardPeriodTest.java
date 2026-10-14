package com.smartsell.controller;

import com.smartsell.exception.InvalidRequestException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("DashboardPeriod")
class DashboardPeriodTest {

    @Test
    @DisplayName("to นับครบทั้งวัน: end คือเที่ยงคืนต้นวันถัดไป")
    void includesWholeLastDay() {
        DashboardPeriod p = DashboardPeriod.of("2026-10-01", "2026-10-31");

        assertThat(p.start()).isEqualTo(LocalDateTime.of(2026, 10, 1, 0, 0));
        assertThat(p.end()).isEqualTo(LocalDateTime.of(2026, 11, 1, 0, 0));
    }

    @Test
    @DisplayName("ไม่ส่งวันที่ = ไม่จำกัดด้านนั้น")
    void missingMeansUnbounded() {
        DashboardPeriod all = DashboardPeriod.of(null, " ");

        assertThat(all.start()).isEqualTo(DashboardPeriod.EARLIEST);
        assertThat(all.end()).isEqualTo(DashboardPeriod.LATEST);
        assertThat(DashboardPeriod.of("2026-01-01", null).end()).isEqualTo(DashboardPeriod.LATEST);
    }

    @Test
    @DisplayName("วันเดียวกันได้ / วันเริ่มหลังวันสิ้นสุดหรือรูปแบบผิดได้ InvalidRequestException")
    void validatesInput() {
        assertThat(DashboardPeriod.of("2026-10-14", "2026-10-14").end()).isEqualTo(LocalDateTime.of(2026, 10, 15, 0, 0));

        assertThatThrownBy(() -> DashboardPeriod.of("2026-10-31", "2026-10-01"))
                .isInstanceOf(InvalidRequestException.class);
        assertThatThrownBy(() -> DashboardPeriod.of("14/10/2569", null))
                .isInstanceOf(InvalidRequestException.class).hasMessageContaining("YYYY-MM-DD");
    }
}
