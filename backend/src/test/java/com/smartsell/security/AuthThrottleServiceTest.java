package com.smartsell.security;

import com.smartsell.exception.TooManyAttemptsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("AuthThrottleService")
class AuthThrottleServiceTest {

    private static final String EMAIL = "owner@shop.com";
    private static final String IP = "203.0.113.10";

    /** นาฬิกาที่เลื่อนเวลาได้เอง ทำให้ทดสอบการหมดอายุหน้าต่าง 15 นาทีได้โดยไม่ต้องรอจริง */
    private static final class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-09-27T10:00:00Z");

        void advance(Duration amount) { now = now.plus(amount); }

        @Override public ZoneId getZone() { return ZoneId.of("UTC"); }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    }

    private MutableClock clock;
    private AuthThrottleService throttle;

    @BeforeEach
    void setUp() {
        clock = new MutableClock();
        throttle = new AuthThrottleService(clock);
    }

    private void failLogin(int times) {
        for (int i = 0; i < times; i++) {
            throttle.recordLoginFailure(EMAIL, IP);
        }
    }

    @Test
    @DisplayName("ผิดไม่ถึง 5 ครั้งยังเข้าได้")
    void allowsUnderThreshold() {
        failLogin(4);
        assertThatCode(() -> throttle.ensureLoginAllowed(EMAIL, IP)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("ผิดครบ 5 ครั้งถูกล็อก")
    void blocksAtThreshold() {
        failLogin(5);
        assertThatThrownBy(() -> throttle.ensureLoginAllowed(EMAIL, IP))
                .isInstanceOf(TooManyAttemptsException.class);
    }

    @Test
    @DisplayName("ยังไม่ครบ 15 นาทีต้องยังถูกล็อกอยู่")
    void staysBlockedWithinWindow() {
        failLogin(5);
        clock.advance(Duration.ofMinutes(14));

        assertThatThrownBy(() -> throttle.ensureLoginAllowed(EMAIL, IP))
                .isInstanceOf(TooManyAttemptsException.class);
    }

    @Test
    @DisplayName("ครบ 15 นาทีแล้วต้องปลดล็อกเอง ไม่ต้องให้ใครมาปลดให้")
    void unblocksAfterWindowExpires() {
        failLogin(5);
        clock.advance(Duration.ofMinutes(15).plusSeconds(1));

        assertThatCode(() -> throttle.ensureLoginAllowed(EMAIL, IP)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("บอกเวลาที่ต้องรอกลับไปด้วย เพื่อใช้เป็น Retry-After")
    void reportsRetryAfterSeconds() {
        failLogin(5);
        clock.advance(Duration.ofMinutes(5));

        assertThatThrownBy(() -> throttle.ensureLoginAllowed(EMAIL, IP))
                .isInstanceOfSatisfying(TooManyAttemptsException.class, ex ->
                        assertThat(ex.getRetryAfterSeconds())
                                .isGreaterThan(0)
                                .isLessThanOrEqualTo(Duration.ofMinutes(10).toSeconds()));
    }

    @Test
    @DisplayName("อีเมลอื่นไม่ถูกล็อกตามไปด้วย")
    void blockIsScopedPerEmail() {
        failLogin(5);

        assertThatCode(() -> throttle.ensureLoginAllowed("someone-else@shop.com", "198.51.100.7"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("อีเมลหนึ่งถูกล็อกแล้ว อีเมลอื่นที่ใช้ IP เดียวกันต้องยังล็อกอินได้")
    void oneLockedEmailDoesNotLockTheWholeIp() {
        failLogin(5);

        assertThatCode(() -> throttle.ensureLoginAllowed("colleague@shop.com", IP))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("ไล่เดาหลายบัญชีจาก IP เดียวจนถึงเพดาน IP ต้องถูกบล็อก")
    void blocksWhenIpLimitReached() {
        for (int i = 0; i < AuthThrottleService.MAX_LOGIN_FAILURES_PER_IP; i++) {
            throttle.recordLoginFailure("victim" + i + "@shop.com", IP);
        }

        assertThatThrownBy(() -> throttle.ensureLoginAllowed("another@shop.com", IP))
                .isInstanceOf(TooManyAttemptsException.class);
    }

    @Test
    @DisplayName("ล็อกอินสำเร็จแล้วตัวนับถูกล้าง")
    void clearResetsCounter() {
        failLogin(4);
        throttle.clearLoginFailures(EMAIL, IP);
        failLogin(4);

        assertThatCode(() -> throttle.ensureLoginAllowed(EMAIL, IP)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("ตัวนับที่หมดอายุถูกกวาดทิ้ง ไม่ให้ค้างในหน่วยความจำ")
    void purgeRemovesExpiredWindows() {
        failLogin(5);
        clock.advance(Duration.ofMinutes(16));
        throttle.purgeExpiredWindows();

        assertThatCode(() -> throttle.ensureLoginAllowed(EMAIL, IP)).doesNotThrowAnyException();
    }
}
