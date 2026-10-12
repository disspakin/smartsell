package com.smartsell.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartsell.entity.AppUser;
import com.smartsell.entity.OtpPurpose;
import com.smartsell.repository.AppUserRepository;
import com.smartsell.security.AuthThrottleService;
import com.smartsell.security.JwtUtil;
import com.smartsell.security.Roles;
import com.smartsell.service.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * เทสต์ทั้งเส้นทาง: HTTP → SecurityFilterChain → Controller → Service → ฐานข้อมูล
 *
 * จุดสำคัญคือกลุ่มเทสต์ท้ายไฟล์ที่ยืนยันว่า /api/admin/** ปฏิเสธคำขอที่ไม่มี token
 * และคำขอที่ role ไม่ถึง — เพราะนั่นคือด่านความปลอดภัยจริง ไม่ใช่การซ่อน URL ฝั่งหน้าเว็บ
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Auth API")
class AuthControllerIntegrationTest {

    /** ต้องตรงกับ app.auth.invite-code ใน application-test.properties */
    private static final String INVITE_CODE = "TEST_INVITE_CODE";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private AuthThrottleService throttleService;

    /** ไม่ส่งเมลจริงระหว่างเทสต์ และใช้ดักรหัส OTP ที่ระบบส่งออกไป */
    @MockBean
    private EmailService emailService;

    /**
     * ตัวนับ rate limit เป็น bean ตัวเดียวที่ใช้ร่วมกันทุกเคส และ MockMvc ส่งคำขอ
     * จาก IP เดียวกันเสมอ ถ้าไม่ล้างก่อน เคสหลังๆ จะถูกล็อกจากคำขอของเคสก่อนหน้า
     */
    @BeforeEach
    void resetThrottle() {
        throttleService.resetAll();
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    private void register(String email) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", email, "password", "SuperSecret1", "displayName", "ร้านทดสอบ", "inviteCode", INVITE_CODE))))
                .andExpect(status().isCreated());
    }

    /** รหัสยืนยันอีเมลล่าสุดที่ระบบส่งไปหาอีเมลนี้ */
    private String lastOtpSentTo(String email) {
        return lastOtpSentTo(email, OtpPurpose.VERIFY_EMAIL);
    }

    /** รหัส OTP ล่าสุดของงานที่ระบุที่ระบบส่งไปหาอีเมลนี้ */
    private String lastOtpSentTo(String email, OtpPurpose purpose) {
        ArgumentCaptor<String> code = ArgumentCaptor.forClass(String.class);
        verify(emailService, atLeastOnce()).sendOtpEmail(eq(email), code.capture(), anyLong(), eq(purpose));
        List<String> codes = code.getAllValues();
        return codes.get(codes.size() - 1);
    }

    private ResultActions verifyOtp(String email, String code) throws Exception {
        return mockMvc.perform(post("/api/auth/verify-otp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("email", email, "code", code))));
    }

    private ResultActions resendOtp(String email) throws Exception {
        return mockMvc.perform(post("/api/auth/resend-otp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("email", email))));
    }

    /** OTP 6 หลักที่ไม่ตรงกับรหัสจริง */
    private String wrongCodeFor(String realCode) {
        return realCode.equals("000000") ? "111111" : "000000";
    }

    private void registerAndVerify(String email) throws Exception {
        register(email);
        verifyOtp(email, lastOtpSentTo(email)).andExpect(status().isOk());
    }

    /** สมัคร ยืนยันอีเมล แล้วล็อกอินต่อ เพราะ /register ไม่ออก token ให้ */
    private String registerAndGetToken(String email) throws Exception {
        registerAndVerify(email);

        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", email, "password", "SuperSecret1"))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(body).get("token").asText();
    }

    // ---------- POST /api/auth/register ----------

    @Test
    @DisplayName("สมัครสำเร็จได้ 201 พร้อมข้อมูลผู้ใช้ และ role STORE_MANAGER")
    void registerSucceeds() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", "new@shop.com", "password", "SuperSecret1", "displayName", "ร้านใหม่", "inviteCode", INVITE_CODE))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.email").value("new@shop.com"))
                .andExpect(jsonPath("$.displayName").value("ร้านใหม่"))
                .andExpect(jsonPath("$.role").value(Roles.STORE_MANAGER));
    }

    @Test
    @DisplayName("สมัครเสร็จต้องไม่ได้ token กลับมา ผู้สมัครต้องไปล็อกอินเอง")
    void registerDoesNotReturnToken() throws Exception {
        String body = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", "plain@shop.com", "password", "SuperSecret1", "displayName", "ร้านใหม่", "inviteCode", INVITE_CODE))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").doesNotExist())
                .andReturn().getResponse().getContentAsString();

        assertThat(body).doesNotContain("token");
    }

    @Test
    @DisplayName("response ต้องไม่มีรหัสผ่านหรือ hash ติดออกไป")
    void registerResponseLeaksNoPassword() throws Exception {
        String body = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", "leak@shop.com", "password", "SuperSecret1", "displayName", "ร้านใหม่", "inviteCode", INVITE_CODE))))
                .andReturn().getResponse().getContentAsString();

        assertThat(body).doesNotContain("SuperSecret1");
        assertThat(body).doesNotContain("passwordHash");
        assertThat(body).doesNotContain("$2a$");
    }

    @Test
    @DisplayName("แม้ผู้เรียกแนบ role มาใน request body ก็ยกระดับสิทธิ์ตัวเองไม่ได้")
    void registerIgnoresRoleFromRequestBody() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", "sneaky@shop.com", "password", "SuperSecret1",
                                "displayName", "ร้านใหม่", "inviteCode", INVITE_CODE, "role", "ADMIN"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value(Roles.STORE_MANAGER));
    }

    @Test
    @DisplayName("รหัสผ่านต้องถูก hash ก่อนลงฐานข้อมูล")
    void registerStoresHashedPassword() throws Exception {
        registerAndGetToken("hash@shop.com");

        AppUser saved = appUserRepository.findByEmail("hash@shop.com").orElseThrow();
        assertThat(saved.getPasswordHash()).isNotEqualTo("SuperSecret1");
        assertThat(passwordEncoder.matches("SuperSecret1", saved.getPasswordHash())).isTrue();
    }

    @Test
    @DisplayName("ข้อมูลไม่ผ่าน validation ได้ 400 พร้อมข้อความรายฟิลด์")
    void registerRejectsInvalidPayload() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", "ไม่ใช่อีเมล", "password", "123", "displayName", "", "inviteCode", INVITE_CODE))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors.email").isNotEmpty())
                .andExpect(jsonPath("$.fieldErrors.password").isNotEmpty())
                .andExpect(jsonPath("$.fieldErrors.displayName").isNotEmpty());
    }

    @Test
    @DisplayName("สมัครด้วยอีเมลซ้ำได้ 409")
    void registerRejectsDuplicateEmail() throws Exception {
        registerAndGetToken("dup@shop.com");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", "dup@shop.com", "password", "SuperSecret1", "displayName", "ร้านซ้ำ", "inviteCode", INVITE_CODE))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    // ---------- POST /api/auth/login ----------

    @Test
    @DisplayName("เข้าสู่ระบบด้วยข้อมูลถูกต้องได้ 200 พร้อม token")
    void loginSucceeds() throws Exception {
        registerAndGetToken("login@shop.com");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", "login@shop.com", "password", "SuperSecret1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.role").value(Roles.STORE_MANAGER));
    }

    @Test
    @DisplayName("อีเมลต่างตัวพิมพ์ต้องเข้าสู่ระบบได้")
    void loginIsCaseInsensitive() throws Exception {
        registerAndGetToken("case@shop.com");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", "CASE@Shop.com", "password", "SuperSecret1"))))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("รหัสผ่านผิดได้ 401")
    void loginRejectsWrongPassword() throws Exception {
        registerAndGetToken("wrong@shop.com");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", "wrong@shop.com", "password", "NotThePassword"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("ล็อกอินผิดครบ 5 ครั้งได้ 429 พร้อม Retry-After แม้ครั้งถัดไปจะใส่รหัสถูก")
    void loginIsRateLimitedAfterFiveFailures() throws Exception {
        registerAndGetToken("throttle@shop.com");

        for (int attempt = 1; attempt <= 5; attempt++) {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of("email", "throttle@shop.com", "password", "WrongPassword"))))
                    .andExpect(status().isUnauthorized());
        }

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", "throttle@shop.com", "password", "SuperSecret1"))))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.status").value(429));
    }

    @Test
    @DisplayName("สมัครถี่เกินโควตาต่อ IP ได้ 429")
    void registerIsRateLimitedPerIp() throws Exception {
        for (int i = 1; i <= 5; i++) {
            registerAndGetToken("bulk" + i + "@shop.com");
        }

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", "bulk6@shop.com", "password", "SuperSecret1", "displayName", "ร้านที่หก", "inviteCode", INVITE_CODE))))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.status").value(429));
    }

    @Test
    @DisplayName("สมัครโดยไม่ส่ง invite code ได้ 403 และไม่สร้างบัญชี")
    void registerWithoutInviteCodeIsForbidden() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", "nocode@shop.com", "password", "SuperSecret1", "displayName", "ร้าน"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));

        assertThat(appUserRepository.findByEmail("nocode@shop.com")).isEmpty();
    }

    @Test
    @DisplayName("สมัครด้วย invite code ผิดได้ 403 แม้อีเมลนั้นมีบัญชีแล้ว (ไม่หลุด 409)")
    void registerWithWrongInviteCodeIsForbiddenEvenForExistingEmail() throws Exception {
        register("taken@shop.com");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", "taken@shop.com", "password", "SuperSecret1",
                                "displayName", "ร้าน", "inviteCode", "WRONG"))))
                .andExpect(status().isForbidden());
    }

    // ---------- OTP ยืนยันอีเมล ----------

    @Test
    @DisplayName("สมัครแล้วต้องส่ง OTP 6 หลักไปที่อีเมล และบัญชียังไม่ยืนยัน")
    void registerSendsOtpAndLeavesAccountUnverified() throws Exception {
        register("otp@shop.com");

        assertThat(lastOtpSentTo("otp@shop.com")).matches("\\d{6}");
        assertThat(appUserRepository.findByEmail("otp@shop.com").orElseThrow().isEmailVerified()).isFalse();
    }

    @Test
    @DisplayName("ยังไม่ยืนยันอีเมลแล้วล็อกอินได้ 403 พร้อม code EMAIL_NOT_VERIFIED และไม่มี token")
    void loginBeforeVerifyIsForbidden() throws Exception {
        register("unverified@shop.com");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", "unverified@shop.com", "password", "SuperSecret1"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("EMAIL_NOT_VERIFIED"))
                .andExpect(jsonPath("$.token").doesNotExist());
    }

    @Test
    @DisplayName("กรอก OTP ถูกแล้วล็อกอินได้ และใช้รหัสเดิมซ้ำไม่ได้")
    void verifyOtpThenLoginSucceeds() throws Exception {
        register("verify@shop.com");
        String code = lastOtpSentTo("verify@shop.com");

        verifyOtp("verify@shop.com", code).andExpect(status().isOk());
        assertThat(appUserRepository.findByEmail("verify@shop.com").orElseThrow().isEmailVerified()).isTrue();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", "verify@shop.com", "password", "SuperSecret1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    @DisplayName("OTP ผิดได้ 400 และบอกจำนวนครั้งที่เหลือ")
    void verifyOtpRejectsWrongCode() throws Exception {
        register("wrongotp@shop.com");
        String wrong = wrongCodeFor(lastOtpSentTo("wrongotp@shop.com"));

        verifyOtp("wrongotp@shop.com", wrong)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("รหัส OTP ไม่ถูกต้อง เหลือโอกาสอีก 4 ครั้ง"));
    }

    @Test
    @DisplayName("กรอก OTP ผิดครบ 5 ครั้ง รหัสจริงก็ใช้ไม่ได้แล้ว")
    void otpIsBurnedAfterFiveWrongAttempts() throws Exception {
        register("burn@shop.com");
        String code = lastOtpSentTo("burn@shop.com");

        for (int attempt = 1; attempt <= 5; attempt++) {
            verifyOtp("burn@shop.com", wrongCodeFor(code)).andExpect(status().isBadRequest());
        }

        verifyOtp("burn@shop.com", code).andExpect(status().isBadRequest());
        assertThat(appUserRepository.findByEmail("burn@shop.com").orElseThrow().isEmailVerified()).isFalse();
    }

    @Test
    @DisplayName("OTP ต้องเป็นตัวเลข 6 หลัก ไม่งั้นได้ 400 จาก validation")
    void verifyOtpValidatesFormat() throws Exception {
        verifyOtp("any@shop.com", "12ab")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.code").exists());
    }

    @Test
    @DisplayName("ขอ OTP ใหม่ภายใน 60 วินาทีได้ 429 พร้อม Retry-After")
    void resendOtpHasCooldown() throws Exception {
        register("cooldown@shop.com");

        resendOtp("cooldown@shop.com")
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"));
    }

    @Test
    @DisplayName("ขอ OTP ใหม่ให้อีเมลที่ไม่มีบัญชีได้ 200 ข้อความเดียวกัน และไม่ส่งเมล")
    void resendOtpForUnknownEmailIsSilent() throws Exception {
        resendOtp("nobody@shop.com")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").isNotEmpty());

        verify(emailService, never()).sendOtpEmail(eq("nobody@shop.com"), anyString(), anyLong(), any());
    }

    // ---------- POST /api/auth/forgot-password + /api/auth/reset-password ----------

    private ResultActions forgotPassword(String email) throws Exception {
        return mockMvc.perform(post("/api/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("email", email))));
    }

    private ResultActions resetPassword(String email, String code, String newPassword) throws Exception {
        return mockMvc.perform(post("/api/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("email", email, "code", code, "newPassword", newPassword))));
    }

    private ResultActions login(String email, String password) throws Exception {
        return mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("email", email, "password", password))));
    }

    /**
     * สมัคร ยืนยันอีเมล แล้วขอรหัสรีเซ็ต — ขอได้ทันทีโดยไม่ติด cooldown 60 วินาที
     * เพราะยืนยันอีเมลสำเร็จแล้วรหัสเดิมถูกลบทิ้ง ไม่มีรหัสล่าสุดให้นับ cooldown
     */
    private void registerVerifyAndForget(String email) throws Exception {
        registerAndVerify(email);
        forgotPassword(email).andExpect(status().isOk());
    }

    @Test
    @DisplayName("ขอรหัสรีเซ็ตแล้วตั้งรหัสใหม่ได้ รหัสเก่าใช้ไม่ได้ รหัสใหม่ใช้ได้")
    void resetPasswordChangesPassword() throws Exception {
        registerVerifyAndForget("reset@shop.com");
        String code = lastOtpSentTo("reset@shop.com", OtpPurpose.RESET_PASSWORD);

        resetPassword("reset@shop.com", code, "BrandNew123").andExpect(status().isOk());

        login("reset@shop.com", "SuperSecret1").andExpect(status().isUnauthorized());
        login("reset@shop.com", "BrandNew123").andExpect(status().isOk());
    }

    @Test
    @DisplayName("ใช้รหัสรีเซ็ตเดิมซ้ำไม่ได้")
    void resetCodeIsSingleUse() throws Exception {
        registerVerifyAndForget("once@shop.com");
        String code = lastOtpSentTo("once@shop.com", OtpPurpose.RESET_PASSWORD);

        resetPassword("once@shop.com", code, "BrandNew123").andExpect(status().isOk());
        resetPassword("once@shop.com", code, "Another999").andExpect(status().isBadRequest());

        login("once@shop.com", "BrandNew123").andExpect(status().isOk());
    }

    @Test
    @DisplayName("OTP ผิดได้ 400 และรหัสผ่านเดิมยังใช้ได้")
    void resetPasswordRejectsWrongCode() throws Exception {
        registerVerifyAndForget("keep@shop.com");
        String wrong = wrongCodeFor(lastOtpSentTo("keep@shop.com", OtpPurpose.RESET_PASSWORD));

        resetPassword("keep@shop.com", wrong, "BrandNew123")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("รหัส OTP ไม่ถูกต้อง เหลือโอกาสอีก 4 ครั้ง"));

        AppUser saved = appUserRepository.findByEmail("keep@shop.com").orElseThrow();
        assertThat(passwordEncoder.matches("SuperSecret1", saved.getPasswordHash())).isTrue();
    }

    @Test
    @DisplayName("กรอกรหัสรีเซ็ตผิดครบ 5 ครั้ง รหัสจริงก็ใช้ไม่ได้ และรหัสผ่านเดิมยังใช้ได้")
    void resetCodeIsBurnedAfterFiveWrongAttempts() throws Exception {
        registerVerifyAndForget("burnreset@shop.com");
        String code = lastOtpSentTo("burnreset@shop.com", OtpPurpose.RESET_PASSWORD);

        for (int attempt = 1; attempt <= 5; attempt++) {
            resetPassword("burnreset@shop.com", wrongCodeFor(code), "BrandNew123").andExpect(status().isBadRequest());
        }

        resetPassword("burnreset@shop.com", code, "BrandNew123").andExpect(status().isBadRequest());
        login("burnreset@shop.com", "SuperSecret1").andExpect(status().isOk());
    }

    @Test
    @DisplayName("รหัสยืนยันอีเมลเอามาตั้งรหัสผ่านใหม่ไม่ได้")
    void verifyEmailCodeCannotResetPassword() throws Exception {
        register("cross@shop.com");
        String verifyCode = lastOtpSentTo("cross@shop.com");

        resetPassword("cross@shop.com", verifyCode, "BrandNew123").andExpect(status().isBadRequest());

        AppUser saved = appUserRepository.findByEmail("cross@shop.com").orElseThrow();
        assertThat(passwordEncoder.matches("SuperSecret1", saved.getPasswordHash())).isTrue();
    }

    @Test
    @DisplayName("ไม่ต้องใช้ invite code แล้ว — รู้ invite code อย่างเดียวตั้งรหัสใหม่ให้บัญชีคนอื่นไม่ได้")
    void inviteCodeNoLongerResetsPassword() throws Exception {
        registerAndVerify("victim2@shop.com");

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", "victim2@shop.com", "inviteCode", INVITE_CODE, "newPassword", "Hijacked123"))))
                .andExpect(status().isBadRequest());

        login("victim2@shop.com", "SuperSecret1").andExpect(status().isOk());
    }

    @Test
    @DisplayName("ขอรหัสรีเซ็ต อีเมลที่มีกับไม่มีบัญชีต้องได้ response เหมือนกันทุกตัวอักษร")
    void forgotPasswordResponseDoesNotRevealAccountExistence() throws Exception {
        registerAndVerify("real@shop.com");

        String existing = forgotPassword("real@shop.com")
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String missing = forgotPassword("ghost@shop.com")
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(missing).isEqualTo(existing);
        verify(emailService, never()).sendOtpEmail(eq("ghost@shop.com"), anyString(), anyLong(), any());
    }

    @Test
    @DisplayName("รหัสใหม่สั้นกว่า 8 ตัวได้ 400 เหมือนตอนสมัคร")
    void resetPasswordEnforcesMinimumLength() throws Exception {
        resetPassword("any@shop.com", "123456", "short")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.newPassword").isNotEmpty());
    }

    @Test
    @DisplayName("ขอรหัสรีเซ็ตอีเมลเดิมเกิน 5 ครั้งได้ 429")
    void forgotPasswordIsRateLimited() throws Exception {
        for (int i = 1; i <= 5; i++) {
            forgotPassword("victim@shop.com").andExpect(status().isOk());
        }

        forgotPassword("victim@shop.com")
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"));
    }

    // ---------- GET /api/auth/me ----------

    @Test
    @DisplayName("เรียก /me โดยไม่มี token ได้ 401")
    void meRequiresToken() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("เรียก /me ด้วย token ปลอมได้ 401")
    void meRejectsForgedToken() throws Exception {
        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer not.a.real.token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("เรียก /me ด้วย token ที่ถูกต้องได้ข้อมูลผู้ใช้ โดยไม่มี hash ติดมา")
    void meReturnsProfile() throws Exception {
        String token = registerAndGetToken("me@shop.com");

        String body = mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("me@shop.com"))
                .andExpect(jsonPath("$.role").value(Roles.STORE_MANAGER))
                .andReturn().getResponse().getContentAsString();

        assertThat(body).doesNotContain("passwordHash");
    }

    // ---------- ด่านจริง: /api/admin/** ----------

    @Test
    @DisplayName("เข้า /api/admin/** โดยไม่มี token ได้ 401 แม้จะรู้ URL")
    void adminEndpointRejectsAnonymous() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard/summary"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("เข้า /api/admin/** ด้วย role ที่ไม่ใช่ผู้ดูแลร้านได้ 403")
    void adminEndpointRejectsInsufficientRole() throws Exception {
        AppUser customer = new AppUser();
        customer.setEmail("customer@shop.com");
        customer.setPasswordHash(passwordEncoder.encode("SuperSecret1"));
        customer.setDisplayName("ลูกค้า");
        customer.setRole("CUSTOMER");
        appUserRepository.saveAndFlush(customer);

        String token = jwtUtil.generateToken(customer.getEmail(), customer.getRole(), customer.getDisplayName());

        mockMvc.perform(get("/api/admin/dashboard/summary").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("เข้า /api/admin/** ด้วย role STORE_MANAGER ได้ 200")
    void adminEndpointAllowsStoreManager() throws Exception {
        String token = registerAndGetToken("manager@shop.com");

        mockMvc.perform(get("/api/admin/dashboard/summary").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }
}
