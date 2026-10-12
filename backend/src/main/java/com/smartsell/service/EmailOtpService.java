package com.smartsell.service;

import com.smartsell.entity.AppUser;
import com.smartsell.entity.EmailOtp;
import com.smartsell.entity.OtpPurpose;
import com.smartsell.exception.InvalidOtpException;
import com.smartsell.exception.TooManyAttemptsException;
import com.smartsell.repository.AppUserRepository;
import com.smartsell.repository.EmailOtpRepository;
import com.smartsell.security.AuthThrottleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;

/**
 * ออกและตรวจรหัส OTP 6 หลัก ใช้ทั้งยืนยันอีเมลหลังสมัคร และตั้งรหัสผ่านใหม่
 *
 * - เก็บแค่ BCrypt hash ของรหัส ใช้ PasswordEncoder ตัวเดียวกับรหัสผ่าน
 * - รหัสหมดอายุใน 5 นาที และกรอกผิดได้ 5 ครั้ง ครั้งที่ 5 รหัสนั้นจะใช้ไม่ได้อีก
 * - ขอรหัสใหม่ได้ทุก 60 วินาที รหัสเดิมจะถูกลบทิ้งทันทีที่ออกรหัสใหม่
 * - รหัสผูกกับงาน (OtpPurpose) รหัสยืนยันอีเมลเอาไปตั้งรหัสผ่านใหม่ไม่ได้
 */
@Service
public class EmailOtpService {

    static final Duration OTP_TTL = Duration.ofMinutes(5);
    static final Duration RESEND_COOLDOWN = Duration.ofSeconds(60);
    static final int MAX_FAILED_ATTEMPTS = 5;

    private static final String INVALID_CODE_MESSAGE = "รหัส OTP ไม่ถูกต้องหรือหมดอายุแล้ว";

    private static final SecureRandom RANDOM = new SecureRandom();

    private final EmailOtpRepository otpRepository;
    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final AuthThrottleService throttleService;
    private final Clock clock;

    @Autowired
    public EmailOtpService(EmailOtpRepository otpRepository, AppUserRepository appUserRepository,
                           PasswordEncoder passwordEncoder, EmailService emailService,
                           AuthThrottleService throttleService) {
        this(otpRepository, appUserRepository, passwordEncoder, emailService, throttleService, Clock.systemDefaultZone());
    }

    /** ใช้ในเทสต์เพื่อเลื่อนเวลาได้โดยไม่ต้องรอจริง */
    EmailOtpService(EmailOtpRepository otpRepository, AppUserRepository appUserRepository,
                    PasswordEncoder passwordEncoder, EmailService emailService,
                    AuthThrottleService throttleService, Clock clock) {
        this.otpRepository = otpRepository;
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.throttleService = throttleService;
        this.clock = clock;
    }

    // ---------- ออกรหัส ----------

    /**
     * ออกรหัสใหม่ให้บัญชีนี้แล้วส่งเมล — รหัสเก่า (ถ้ามี ไม่ว่างานไหน) ใช้ไม่ได้อีก
     * ต้องถูกเรียกภายใน transaction ของผู้เรียก (AuthService.register / resend / requestPasswordReset)
     */
    @Transactional
    public void issueFor(AppUser user, OtpPurpose purpose) {
        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        LocalDateTime now = LocalDateTime.now(clock);

        otpRepository.deleteByUserId(user.getId());

        EmailOtp otp = new EmailOtp();
        otp.setUser(user);
        otp.setPurpose(purpose);
        otp.setCodeHash(passwordEncoder.encode(code));
        otp.setCreatedAt(now);
        otp.setExpiresAt(now.plus(OTP_TTL));
        otpRepository.save(otp);

        emailService.sendOtpEmail(user.getEmail(), code, OTP_TTL.toMinutes(), purpose);
    }

    /**
     * ขอรหัสยืนยันอีเมลใหม่ — อีเมลที่ไม่มีบัญชีหรือยืนยันแล้วจะเงียบไป ไม่โยน error
     * ผู้เรียกจึงใช้ endpoint นี้ไล่เช็คว่าอีเมลไหนมีบัญชีไม่ได้
     */
    @Transactional
    public void resend(String rawEmail) {
        String email = normalizeEmail(rawEmail);
        AppUser user = appUserRepository.findByEmail(email).orElse(null);
        if (user == null || user.isEmailVerified()) {
            return;
        }

        ensureCooldownPassed(user);

        // cooldown กันกดรัว ส่วนโควตานี้กันคนใช้เราเป็นเครื่องยิงเมลใส่กล่องจดหมายคนอื่นทั้งวัน
        throttleService.ensureOtpResendAllowed(email);
        throttleService.recordOtpResend(email);

        issueFor(user, OtpPurpose.VERIFY_EMAIL);
    }

    /**
     * ส่งรหัสตั้งรหัสผ่านใหม่ — อีเมลที่ไม่มีบัญชีจะเงียบไป เหตุผลเดียวกับ resend
     * โควตาต่ออีเมล/IP ตรวจที่ AuthService.requestPasswordReset ก่อนเข้ามาถึงนี่แล้ว
     */
    @Transactional
    public void requestPasswordReset(String rawEmail) {
        Optional<AppUser> user = appUserRepository.findByEmail(normalizeEmail(rawEmail));
        if (user.isEmpty()) {
            return;
        }
        ensureCooldownPassed(user.get());
        issueFor(user.get(), OtpPurpose.RESET_PASSWORD);
    }

    // ---------- ตรวจรหัส ----------

    /**
     * ตรวจรหัสยืนยันอีเมล ถูกแล้วบัญชีจะเป็น email_verified = true และรหัสถูกลบทิ้ง
     *
     * noRollbackFor จำเป็น — ไม่งั้นตอนรหัสผิด exception จะ rollback การเพิ่ม failedAttempts ไปด้วย
     * แล้วตัวนับจะไม่ขยับเลย เดาได้ไม่จำกัด
     */
    @Transactional(noRollbackFor = InvalidOtpException.class)
    public void verify(String rawEmail, String code) {
        AppUser user = appUserRepository.findByEmail(normalizeEmail(rawEmail))
                .orElseThrow(() -> new InvalidOtpException(INVALID_CODE_MESSAGE));
        if (user.isEmailVerified()) {
            return;
        }

        consume(user, code, OtpPurpose.VERIFY_EMAIL);
        user.setEmailVerified(true);
        appUserRepository.save(user);
    }

    /**
     * ตรวจรหัสตั้งรหัสผ่านใหม่ — ถูกแล้วคืนบัญชีให้ AuthService เปลี่ยนรหัสผ่านต่อ
     *
     * ผู้เรียกต้องใส่ noRollbackFor = InvalidOtpException.class ที่ transaction ของตัวเองด้วย
     * ไม่งั้น transaction ชั้นนอกจะ rollback การนับครั้งที่ผิดทิ้งตอน exception วิ่งผ่าน
     */
    @Transactional(noRollbackFor = InvalidOtpException.class)
    public AppUser consumePasswordReset(String rawEmail, String code) {
        AppUser user = appUserRepository.findByEmail(normalizeEmail(rawEmail))
                .orElseThrow(() -> new InvalidOtpException(INVALID_CODE_MESSAGE));
        consume(user, code, OtpPurpose.RESET_PASSWORD);
        return user;
    }

    // ---------- ภายใน ----------

    /** ตรวจรหัสของงานที่ระบุ ผิดจะนับครั้งและโยน InvalidOtpException ถูกแล้วลบรหัสทิ้ง */
    private void consume(AppUser user, String code, OtpPurpose purpose) {
        // รหัสของงานอื่นถือว่าไม่มีรหัส — ไม่นับครั้งผิด เพราะไม่ได้เดารหัสตัวนั้นจริง
        EmailOtp otp = otpRepository.findByUserIdForUpdate(user.getId())
                .filter(o -> o.getPurpose() == purpose)
                .orElseThrow(() -> new InvalidOtpException(INVALID_CODE_MESSAGE));

        if (!LocalDateTime.now(clock).isBefore(otp.getExpiresAt())) {
            throw new InvalidOtpException("รหัส OTP หมดอายุแล้ว กรุณาขอรหัสใหม่");
        }
        if (otp.getFailedAttempts() >= MAX_FAILED_ATTEMPTS) {
            throw new InvalidOtpException("กรอกรหัสผิดครบ " + MAX_FAILED_ATTEMPTS + " ครั้งแล้ว กรุณาขอรหัสใหม่");
        }

        if (code == null || !passwordEncoder.matches(code.trim(), otp.getCodeHash())) {
            otp.setFailedAttempts(otp.getFailedAttempts() + 1);
            otpRepository.save(otp);

            int remaining = MAX_FAILED_ATTEMPTS - otp.getFailedAttempts();
            if (remaining <= 0) {
                throw new InvalidOtpException("กรอกรหัสผิดครบ " + MAX_FAILED_ATTEMPTS + " ครั้งแล้ว กรุณาขอรหัสใหม่");
            }
            throw new InvalidOtpException("รหัส OTP ไม่ถูกต้อง เหลือโอกาสอีก " + remaining + " ครั้ง");
        }

        otpRepository.delete(otp);
    }

    /** ขอรหัสใหม่ก่อนครบ 60 วินาทีนับจากรหัสล่าสุด (งานไหนก็ได้) จะได้ 429 พร้อมเวลาที่ต้องรอ */
    private void ensureCooldownPassed(AppUser user) {
        otpRepository.findByUserId(user.getId()).ifPresent(existing -> {
            LocalDateTime allowedAt = existing.getCreatedAt().plus(RESEND_COOLDOWN);
            LocalDateTime now = LocalDateTime.now(clock);
            if (now.isBefore(allowedAt)) {
                long secondsLeft = Math.max(1, Duration.between(now, allowedAt).toSeconds());
                throw new TooManyAttemptsException(
                        "กรุณารอ " + secondsLeft + " วินาทีก่อนขอรหัสใหม่", secondsLeft);
            }
        });
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}
