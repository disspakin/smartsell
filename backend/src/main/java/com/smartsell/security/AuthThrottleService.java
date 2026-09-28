package com.smartsell.security;

import com.smartsell.exception.TooManyAttemptsException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * จำกัดจำนวนครั้งที่เรียก /api/auth/login, /api/auth/register และ /api/auth/reset-password ได้ในช่วงเวลาหนึ่ง
 *
 * ใช้หน้าต่างเวลาแบบตายตัว (fixed window): นับครั้งแรกเป็นจุดเริ่มหน้าต่าง 15 นาที
 * ครบโควตาเมื่อไหร่ก็ปฏิเสธจนกว่าหน้าต่างจะหมดอายุ
 *
 * ข้อจำกัดที่ต้องรู้:
 * - เก็บใน memory ของ process เดียว รีสตาร์ตแล้วตัวนับหายหมด และถ้ารันหลาย instance
 *   แต่ละตัวจะนับแยกกัน (ผู้โจมตีจะได้โควตาเท่ากับจำนวน instance)
 *   ถ้าต้องการให้ทนกว่านี้ต้องย้ายตัวนับไป Redis
 * - การล็อกด้วยอีเมลทำให้คนอื่นจงใจยิงรหัสผิดใส่อีเมลเราเพื่อล็อกบัญชีเราได้
 *   จึงล็อกแค่ชั่วคราว 15 นาที ไม่ล็อกถาวร และนับแยกตาม IP ควบคู่ไปด้วย
 */
@Service
public class AuthThrottleService {

    /** จำนวนครั้งที่ล็อกอินผิดได้ต่อ "หนึ่งอีเมล" ก่อนถูกล็อกชั่วคราว */
    static final int MAX_LOGIN_FAILURES = 5;

    /**
     * เพดานต่อ "หนึ่ง IP" ตั้งไว้สูงกว่าเพดานต่ออีเมลมาก
     *
     * ด่านหลักคือเพดานต่ออีเมล (5 ครั้งตามข้อกำหนด) ส่วนเพดานต่อ IP มีไว้จับกรณีไล่เดา
     * หลายบัญชีจากที่เดียว ถ้าตั้งสองค่าเท่ากันจะเกิดปัญหาว่าคนที่ใช้ IP ร่วมกัน
     * (เครื่องเดียวกัน, เครือข่ายเดียวกัน, หรือทุกคนเมื่ออยู่หลัง reverse proxy)
     * ถูกล็อกยกชุดเพราะมีคนใดคนหนึ่งพิมพ์รหัสผิด
     */
    static final int MAX_LOGIN_FAILURES_PER_IP = 20;

    /** จำนวนบัญชีที่สมัครได้จาก IP เดียวกันภายในหนึ่งหน้าต่าง */
    static final int MAX_REGISTRATIONS_PER_IP = 5;

    /** จำนวนครั้งที่ขอรีเซ็ตรหัสผ่านได้ต่อ "หนึ่งอีเมล" และต่อ "หนึ่ง IP" ภายในหนึ่งหน้าต่าง */
    static final int MAX_RESET_ATTEMPTS_PER_EMAIL = 5;
    static final int MAX_RESET_ATTEMPTS_PER_IP = 10;

    static final Duration WINDOW = Duration.ofMinutes(15);

    private final Map<String, Window> windows = new ConcurrentHashMap<>();
    private final Clock clock;

    public AuthThrottleService() {
        this(Clock.systemUTC());
    }

    /** ใช้ในเทสต์เพื่อเลื่อนเวลาได้โดยไม่ต้องรอจริง */
    AuthThrottleService(Clock clock) {
        this.clock = clock;
    }

    private record Window(Instant startedAt, int count) {
        Window increment() {
            return new Window(startedAt, count + 1);
        }
    }

    // ---------- login ----------

    /** เรียกก่อนตรวจรหัสผ่าน — โยน TooManyAttemptsException ถ้าถูกล็อกอยู่ */
    public void ensureLoginAllowed(String email, String clientIp) {
        ensureUnderLimit(loginEmailKey(email), MAX_LOGIN_FAILURES,
                "พยายามเข้าสู่ระบบผิดหลายครั้งเกินไป กรุณารอ %d นาทีแล้วลองใหม่");
        ensureUnderLimit(loginIpKey(clientIp), MAX_LOGIN_FAILURES_PER_IP,
                "มีการพยายามเข้าสู่ระบบผิดจำนวนมากจากเครือข่ายนี้ กรุณารอ %d นาทีแล้วลองใหม่");
    }

    public void recordLoginFailure(String email, String clientIp) {
        increment(loginEmailKey(email));
        increment(loginIpKey(clientIp));
    }

    /** ล็อกอินสำเร็จแล้วให้ล้างตัวนับ ผู้ใช้ที่พิมพ์ผิดไม่กี่ครั้งจะไม่สะสมค้างไว้ */
    public void clearLoginFailures(String email, String clientIp) {
        windows.remove(loginEmailKey(email));
        windows.remove(loginIpKey(clientIp));
    }

    // ---------- register ----------

    public void ensureRegisterAllowed(String clientIp) {
        ensureUnderLimit(registerKey(clientIp), MAX_REGISTRATIONS_PER_IP,
                "สมัครบัญชีถี่เกินไป กรุณารอ %d นาทีแล้วลองใหม่");
    }

    public void recordRegistration(String clientIp) {
        increment(registerKey(clientIp));
    }

    /**
     * invite code ผิดนับรวมในโควตาสมัครต่อ IP เดียวกัน
     * ผู้ที่ไม่รู้ code จึงเดาได้ไม่เกิน MAX_REGISTRATIONS_PER_IP ครั้งต่อหน้าต่าง
     */
    public void recordRegisterFailure(String clientIp) {
        increment(registerKey(clientIp));
    }

    // ---------- reset password ----------

    /** เรียกก่อนตรวจ invite code — โยน TooManyAttemptsException ถ้าถูกล็อกอยู่ */
    public void ensurePasswordResetAllowed(String email, String clientIp) {
        ensureUnderLimit(resetEmailKey(email), MAX_RESET_ATTEMPTS_PER_EMAIL,
                "ขอรีเซ็ตรหัสผ่านถี่เกินไป กรุณารอ %d นาทีแล้วลองใหม่");
        ensureUnderLimit(resetIpKey(clientIp), MAX_RESET_ATTEMPTS_PER_IP,
                "มีการขอรีเซ็ตรหัสผ่านจำนวนมากจากเครือข่ายนี้ กรุณารอ %d นาทีแล้วลองใหม่");
    }

    /**
     * นับทุกครั้งที่ขอรีเซ็ต ไม่ว่าจะสำเร็จหรือไม่
     * การเปลี่ยนรหัสซ้ำๆ ไม่ใช่พฤติกรรมปกติ และไม่ล้างตัวนับเมื่อสำเร็จ
     * เพราะ response ตั้งใจให้เหมือนกันทุกกรณีอยู่แล้ว
     */
    public void recordPasswordResetAttempt(String email, String clientIp) {
        increment(resetEmailKey(email));
        increment(resetIpKey(clientIp));
    }

    // ---------- ภายใน ----------

    private void ensureUnderLimit(String key, int max, String messageTemplate) {
        Window window = windows.get(key);
        if (window == null) {
            return;
        }

        Instant now = clock.instant();
        Instant expiresAt = window.startedAt().plus(WINDOW);
        if (!expiresAt.isAfter(now)) {
            windows.remove(key);
            return;
        }

        if (window.count() >= max) {
            long secondsLeft = Math.max(1, Duration.between(now, expiresAt).toSeconds());
            long minutesLeft = Math.max(1, (secondsLeft + 59) / 60);
            throw new TooManyAttemptsException(String.format(messageTemplate, minutesLeft), secondsLeft);
        }
    }

    private void increment(String key) {
        Instant now = clock.instant();
        windows.compute(key, (ignored, window) -> {
            if (window == null || !window.startedAt().plus(WINDOW).isAfter(now)) {
                return new Window(now, 1);
            }
            return window.increment();
        });
    }

    private String loginEmailKey(String email) {
        return "login:email:" + (email == null ? "" : email.trim().toLowerCase(Locale.ROOT));
    }

    private String loginIpKey(String clientIp) {
        return "login:ip:" + clientIp;
    }

    private String registerKey(String clientIp) {
        return "register:ip:" + clientIp;
    }

    private String resetEmailKey(String email) {
        return "reset:email:" + (email == null ? "" : email.trim().toLowerCase(Locale.ROOT));
    }

    private String resetIpKey(String clientIp) {
        return "reset:ip:" + clientIp;
    }

    /** กวาดหน้าต่างที่หมดอายุทิ้ง ไม่ให้ map โตไม่สิ้นสุดจาก IP ที่ไม่กลับมาอีก */
    @Scheduled(fixedDelay = 15, initialDelay = 15, timeUnit = java.util.concurrent.TimeUnit.MINUTES)
    void purgeExpiredWindows() {
        Instant now = clock.instant();
        windows.entrySet().removeIf(entry -> !entry.getValue().startedAt().plus(WINDOW).isAfter(now));
    }

    /** ล้างตัวนับทั้งหมด — ใช้ในเทสต์เพื่อไม่ให้สถานะรั่วข้ามเคส */
    public void resetAll() {
        windows.clear();
    }
}
