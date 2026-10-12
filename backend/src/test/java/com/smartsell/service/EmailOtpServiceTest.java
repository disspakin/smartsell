package com.smartsell.service;

import com.smartsell.entity.AppUser;
import com.smartsell.entity.EmailOtp;
import com.smartsell.entity.OtpPurpose;
import com.smartsell.exception.InvalidOtpException;
import com.smartsell.exception.TooManyAttemptsException;
import com.smartsell.repository.AppUserRepository;
import com.smartsell.repository.EmailOtpRepository;
import com.smartsell.security.AuthThrottleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmailOtpService")
class EmailOtpServiceTest {

    private static final String EMAIL = "owner@shop.com";

    /** นาฬิกาที่เลื่อนเวลาได้เอง ทำให้ทดสอบการหมดอายุ 5 นาทีและ cooldown 60 วินาทีได้โดยไม่ต้องรอจริง */
    private static final class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-09-27T10:00:00Z");

        void advance(Duration amount) { now = now.plus(amount); }

        @Override public ZoneId getZone() { return ZoneId.of("UTC"); }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    }

    @Mock
    private EmailOtpRepository otpRepository;

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private EmailService emailService;

    // ใช้ตัวเข้ารหัสจริงเพื่อยืนยันว่าเก็บรหัสเป็น hash และตรวจกับ hash ได้จริง
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private MutableClock clock;
    private EmailOtpService otpService;
    private AppUser user;

    @BeforeEach
    void setUp() {
        clock = new MutableClock();
        otpService = new EmailOtpService(otpRepository, appUserRepository, passwordEncoder,
                emailService, new AuthThrottleService(), clock);

        user = new AppUser();
        user.setId(1L);
        user.setEmail(EMAIL);
        user.setEmailVerified(false);
    }

    /** ออกรหัสแล้วคืนทั้งรหัสที่ส่งทางเมลและแถวที่บันทึก */
    private record Issued(String code, EmailOtp otp) {}

    private Issued issue() {
        return issue(OtpPurpose.VERIFY_EMAIL);
    }

    private Issued issue(OtpPurpose purpose) {
        otpService.issueFor(user, purpose);

        ArgumentCaptor<String> code = ArgumentCaptor.forClass(String.class);
        verify(emailService, atLeastOnce()).sendOtpEmail(eq(EMAIL), code.capture(), anyLong(), any());
        ArgumentCaptor<EmailOtp> saved = ArgumentCaptor.forClass(EmailOtp.class);
        verify(otpRepository, atLeastOnce()).save(saved.capture());

        EmailOtp otp = saved.getValue();
        lenient().when(appUserRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        lenient().when(otpRepository.findByUserIdForUpdate(1L)).thenReturn(Optional.of(otp));
        lenient().when(otpRepository.findByUserId(1L)).thenReturn(Optional.of(otp));
        return new Issued(code.getAllValues().get(code.getAllValues().size() - 1), otp);
    }

    private String wrongCodeFor(String realCode) {
        return realCode.equals("000000") ? "111111" : "000000";
    }

    @Test
    @DisplayName("รหัสเป็นตัวเลข 6 หลัก เก็บเป็น BCrypt hash และหมดอายุใน 5 นาที")
    void issueStoresHashedSixDigitCode() {
        Issued issued = issue();

        assertThat(issued.code()).matches("\\d{6}");
        assertThat(issued.otp().getCodeHash()).isNotEqualTo(issued.code()).startsWith("$2a$");
        assertThat(passwordEncoder.matches(issued.code(), issued.otp().getCodeHash())).isTrue();
        assertThat(Duration.between(issued.otp().getCreatedAt(), issued.otp().getExpiresAt()))
                .isEqualTo(Duration.ofMinutes(5));
        verify(otpRepository).deleteByUserId(1L);
    }

    @Test
    @DisplayName("กรอกรหัสถูกภายใน 5 นาทีแล้วบัญชีถูกยืนยัน และรหัสถูกลบทิ้ง")
    void verifyMarksUserVerified() {
        Issued issued = issue();
        clock.advance(Duration.ofMinutes(4));

        otpService.verify(EMAIL, issued.code());

        assertThat(user.isEmailVerified()).isTrue();
        verify(otpRepository).delete(issued.otp());
    }

    @Test
    @DisplayName("รหัสที่เลย 5 นาทีแล้วใช้ไม่ได้ แม้จะถูกต้อง")
    void expiredCodeIsRejected() {
        Issued issued = issue();
        clock.advance(Duration.ofMinutes(5));

        assertThatThrownBy(() -> otpService.verify(EMAIL, issued.code()))
                .isInstanceOf(InvalidOtpException.class)
                .hasMessageContaining("หมดอายุ");
        assertThat(user.isEmailVerified()).isFalse();
    }

    @Test
    @DisplayName("กรอกผิดแต่ละครั้งต้องนับเพิ่ม ครั้งที่ 5 แล้วรหัสจริงก็ใช้ไม่ได้")
    void fiveWrongAttemptsBurnTheCode() {
        Issued issued = issue();

        for (int attempt = 1; attempt <= 5; attempt++) {
            assertThatThrownBy(() -> otpService.verify(EMAIL, wrongCodeFor(issued.code())))
                    .isInstanceOf(InvalidOtpException.class);
            assertThat(issued.otp().getFailedAttempts()).isEqualTo(attempt);
        }

        assertThatThrownBy(() -> otpService.verify(EMAIL, issued.code()))
                .isInstanceOf(InvalidOtpException.class)
                .hasMessageContaining("ครบ 5 ครั้ง");
        assertThat(user.isEmailVerified()).isFalse();
    }

    @Test
    @DisplayName("ขอรหัสใหม่ก่อนครบ 60 วินาทีได้ 429 พร้อมจำนวนวินาทีที่เหลือ")
    void resendWithinCooldownIsRejected() {
        issue();
        clock.advance(Duration.ofSeconds(20));

        assertThatThrownBy(() -> otpService.resend(EMAIL))
                .isInstanceOf(TooManyAttemptsException.class)
                .satisfies(ex -> assertThat(((TooManyAttemptsException) ex).getRetryAfterSeconds()).isEqualTo(40));
    }

    @Test
    @DisplayName("ครบ 60 วินาทีแล้วขอรหัสใหม่ได้ และรหัสเก่าถูกลบ")
    void resendAfterCooldownIssuesNewCode() {
        issue();
        clock.advance(Duration.ofSeconds(60));

        otpService.resend(EMAIL);

        verify(emailService, times(2)).sendOtpEmail(eq(EMAIL), anyString(), anyLong(), eq(OtpPurpose.VERIFY_EMAIL));
        verify(otpRepository, times(2)).deleteByUserId(1L);
    }

    @Test
    @DisplayName("ขอรหัสใหม่ให้บัญชีที่ยืนยันแล้วหรือไม่มีอยู่ต้องเงียบ ไม่ส่งเมล")
    void resendIsSilentForVerifiedOrUnknownEmail() {
        user.setEmailVerified(true);
        when(appUserRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(appUserRepository.findByEmail("nobody@shop.com")).thenReturn(Optional.empty());

        otpService.resend(EMAIL);
        otpService.resend("nobody@shop.com");

        verify(emailService, never()).sendOtpEmail(anyString(), anyString(), anyLong(), any());
        verify(otpRepository, never()).save(any());
    }

    @Test
    @DisplayName("ขอรหัสใหม่เกิน 5 ครั้งใน 15 นาทีต้องถูกล็อก แม้จะรอ cooldown ครบทุกครั้ง")
    void resendIsCappedPerEmail() {
        issue();
        for (int i = 1; i <= 5; i++) {
            clock.advance(Duration.ofSeconds(61));
            otpService.resend(EMAIL);
        }

        clock.advance(Duration.ofSeconds(61));
        assertThatThrownBy(() -> otpService.resend(EMAIL))
                .isInstanceOf(TooManyAttemptsException.class)
                .hasMessageContaining("บ่อยเกินไป");
    }

    // ---------- ตั้งรหัสผ่านใหม่ ----------

    @Test
    @DisplayName("รหัสตั้งรหัสผ่านใหม่ที่ถูกต้องต้องคืนบัญชีกลับมา และรหัสถูกลบทิ้ง")
    void consumePasswordResetReturnsUser() {
        Issued issued = issue(OtpPurpose.RESET_PASSWORD);

        AppUser result = otpService.consumePasswordReset(EMAIL, issued.code());

        assertThat(result).isSameAs(user);
        verify(otpRepository).delete(issued.otp());
    }

    @Test
    @DisplayName("รหัสยืนยันอีเมลเอาไปตั้งรหัสผ่านใหม่ไม่ได้ และไม่นับเป็นครั้งที่ผิด")
    void verifyEmailCodeCannotResetPassword() {
        Issued issued = issue(OtpPurpose.VERIFY_EMAIL);

        assertThatThrownBy(() -> otpService.consumePasswordReset(EMAIL, issued.code()))
                .isInstanceOf(InvalidOtpException.class);
        assertThat(issued.otp().getFailedAttempts()).isZero();
        verify(otpRepository, never()).delete(any());
    }

    @Test
    @DisplayName("รหัสตั้งรหัสผ่านใหม่เอาไปยืนยันอีเมลไม่ได้")
    void resetCodeCannotVerifyEmail() {
        Issued issued = issue(OtpPurpose.RESET_PASSWORD);

        assertThatThrownBy(() -> otpService.verify(EMAIL, issued.code()))
                .isInstanceOf(InvalidOtpException.class);
        assertThat(user.isEmailVerified()).isFalse();
    }

    @Test
    @DisplayName("ขอรหัสตั้งรหัสผ่านใหม่ต้องส่งเมลแบบ RESET_PASSWORD")
    void requestPasswordResetSendsResetCode() {
        when(appUserRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        otpService.requestPasswordReset("Owner@Shop.com");

        verify(emailService).sendOtpEmail(eq(EMAIL), anyString(), anyLong(), eq(OtpPurpose.RESET_PASSWORD));
    }

    @Test
    @DisplayName("ขอรหัสตั้งรหัสผ่านใหม่ให้อีเมลที่ไม่มีบัญชีต้องเงียบ ไม่ส่งเมล")
    void requestPasswordResetIsSilentForUnknownEmail() {
        when(appUserRepository.findByEmail("nobody@shop.com")).thenReturn(Optional.empty());

        otpService.requestPasswordReset("nobody@shop.com");

        verify(emailService, never()).sendOtpEmail(anyString(), anyString(), anyLong(), any());
    }

    @Test
    @DisplayName("cooldown 60 วินาทีนับรวมทุกงาน — เพิ่งสมัครแล้วขอรหัสรีเซ็ตทันทีต้องรอ")
    void cooldownIsSharedAcrossPurposes() {
        issue(OtpPurpose.VERIFY_EMAIL);
        clock.advance(Duration.ofSeconds(10));

        assertThatThrownBy(() -> otpService.requestPasswordReset(EMAIL))
                .isInstanceOf(TooManyAttemptsException.class);
    }
}
