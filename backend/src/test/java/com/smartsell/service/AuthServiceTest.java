package com.smartsell.service;

import com.smartsell.dto.AuthResponse;
import com.smartsell.dto.LoginRequest;
import com.smartsell.dto.RegisterRequest;
import com.smartsell.dto.ResetPasswordRequest;
import com.smartsell.entity.AppUser;
import com.smartsell.exception.EmailAlreadyUsedException;
import com.smartsell.exception.InvalidCredentialsException;
import com.smartsell.exception.InvalidInviteCodeException;
import com.smartsell.exception.TooManyAttemptsException;
import com.smartsell.repository.AppUserRepository;
import com.smartsell.security.AuthThrottleService;
import com.smartsell.security.JwtUtil;
import com.smartsell.security.Roles;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService")
class AuthServiceTest {

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private JwtUtil jwtUtil;

    // ใช้ตัวเข้ารหัสจริงแทน mock เพื่อให้เทสต์ยืนยันได้ว่ารหัสผ่านถูก hash จริง
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private static final String CLIENT_IP = "203.0.113.10";
    private static final String INVITE_CODE = "TEST_INVITE_CODE";

    private AuthService authService;
    private AuthThrottleService throttleService;

    @BeforeEach
    void setUp() {
        // ใช้ตัวจริงแทน mock เพื่อให้เทสต์ครอบคลุมการทำงานร่วมกับ rate limit ด้วย
        // สร้างใหม่ทุกเคส ตัวนับจึงไม่รั่วข้ามเทสต์
        throttleService = new AuthThrottleService();
        authService = new AuthService(appUserRepository, passwordEncoder, jwtUtil, throttleService, INVITE_CODE);
    }

    private RegisterRequest registerRequest(String email, String password, String displayName) {
        return registerRequest(email, password, displayName, INVITE_CODE);
    }

    private RegisterRequest registerRequest(String email, String password, String displayName, String inviteCode) {
        RegisterRequest request = new RegisterRequest();
        request.setEmail(email);
        request.setPassword(password);
        request.setDisplayName(displayName);
        request.setInviteCode(inviteCode);
        return request;
    }

    private ResetPasswordRequest resetRequest(String email, String inviteCode, String newPassword) {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setEmail(email);
        request.setInviteCode(inviteCode);
        request.setNewPassword(newPassword);
        return request;
    }

    private LoginRequest loginRequest(String email, String password) {
        LoginRequest request = new LoginRequest();
        request.setEmail(email);
        request.setPassword(password);
        return request;
    }

    private AppUser existingUser(String email, String rawPassword, String role) {
        AppUser user = new AppUser();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setDisplayName("ร้านทดสอบ");
        user.setRole(role);
        return user;
    }

    @Test
    @DisplayName("สมัครแล้วต้องเก็บรหัสผ่านเป็น hash ไม่ใช่ข้อความธรรมดา")
    void registerHashesPassword() {
        when(appUserRepository.findByEmail("owner@shop.com")).thenReturn(Optional.empty());

        authService.register(registerRequest("owner@shop.com", "SuperSecret1", "ร้านทดสอบ"), CLIENT_IP);

        ArgumentCaptor<AppUser> saved = ArgumentCaptor.forClass(AppUser.class);
        verify(appUserRepository).saveAndFlush(saved.capture());

        String storedHash = saved.getValue().getPasswordHash();
        assertThat(storedHash).isNotEqualTo("SuperSecret1");
        assertThat(storedHash).startsWith("$2a$");
        assertThat(passwordEncoder.matches("SuperSecret1", storedHash)).isTrue();
    }

    @Test
    @DisplayName("role ต้องถูกกำหนดเป็น STORE_MANAGER ฝั่ง server เสมอ")
    void registerAlwaysAssignsStoreManagerRole() {
        when(appUserRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        AppUser created = authService.register(registerRequest("owner@shop.com", "SuperSecret1", "ร้านทดสอบ"), CLIENT_IP);

        ArgumentCaptor<AppUser> saved = ArgumentCaptor.forClass(AppUser.class);
        verify(appUserRepository).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getRole()).isEqualTo(Roles.STORE_MANAGER);
        assertThat(created.getRole()).isEqualTo(Roles.STORE_MANAGER);
    }

    @Test
    @DisplayName("สมัครเสร็จต้องไม่ออก token ให้ ผู้สมัครต้องไปล็อกอินเอง")
    void registerDoesNotIssueToken() {
        when(appUserRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        authService.register(registerRequest("owner@shop.com", "SuperSecret1", "ร้านทดสอบ"), CLIENT_IP);

        verify(jwtUtil, never()).generateToken(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("อีเมลถูกแปลงเป็นตัวพิมพ์เล็กและตัดช่องว่างก่อนบันทึก")
    void registerNormalizesEmail() {
        when(appUserRepository.findByEmail("owner@shop.com")).thenReturn(Optional.empty());

        authService.register(registerRequest("  Owner@Shop.COM  ", "SuperSecret1", "ร้านทดสอบ"), CLIENT_IP);

        ArgumentCaptor<AppUser> saved = ArgumentCaptor.forClass(AppUser.class);
        verify(appUserRepository).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getEmail()).isEqualTo("owner@shop.com");
    }

    @Test
    @DisplayName("สมัครด้วยอีเมลซ้ำต้องถูกปฏิเสธและไม่บันทึกลงฐานข้อมูล")
    void registerRejectsDuplicateEmail() {
        when(appUserRepository.findByEmail("owner@shop.com"))
                .thenReturn(Optional.of(existingUser("owner@shop.com", "SuperSecret1", Roles.STORE_MANAGER)));

        assertThatThrownBy(() -> authService.register(registerRequest("owner@shop.com", "SuperSecret1", "ร้านทดสอบ"), CLIENT_IP))
                .isInstanceOf(EmailAlreadyUsedException.class);

        verify(appUserRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("เข้าสู่ระบบด้วยข้อมูลถูกต้องต้องได้ token กลับมา")
    void loginReturnsTokenForValidCredentials() {
        AppUser user = existingUser("owner@shop.com", "SuperSecret1", Roles.STORE_MANAGER);
        when(appUserRepository.findByEmail("owner@shop.com")).thenReturn(Optional.of(user));
        when(jwtUtil.generateToken("owner@shop.com", Roles.STORE_MANAGER, "ร้านทดสอบ")).thenReturn("signed-token");

        AuthResponse response = authService.login(loginRequest("owner@shop.com", "SuperSecret1"), CLIENT_IP);

        assertThat(response.getToken()).isEqualTo("signed-token");
        assertThat(response.getEmail()).isEqualTo("owner@shop.com");
        assertThat(response.getRole()).isEqualTo(Roles.STORE_MANAGER);
    }

    @Test
    @DisplayName("รหัสผ่านผิดต้องไม่ออก token ให้")
    void loginRejectsWrongPassword() {
        AppUser user = existingUser("owner@shop.com", "SuperSecret1", Roles.STORE_MANAGER);
        when(appUserRepository.findByEmail("owner@shop.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(loginRequest("owner@shop.com", "WrongPassword"), CLIENT_IP))
                .isInstanceOf(InvalidCredentialsException.class);

        verify(jwtUtil, never()).generateToken(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("อีเมลที่ไม่มีบัญชีกับรหัสผ่านผิดต้องได้ข้อความเดียวกัน กันการไล่เดาว่าอีเมลไหนมีอยู่จริง")
    void loginUsesSameMessageToPreventUserEnumeration() {
        when(appUserRepository.findByEmail("ghost@shop.com")).thenReturn(Optional.empty());
        when(appUserRepository.findByEmail("owner@shop.com"))
                .thenReturn(Optional.of(existingUser("owner@shop.com", "SuperSecret1", Roles.STORE_MANAGER)));

        String unknownEmailMessage = catchMessage(() -> authService.login(loginRequest("ghost@shop.com", "SuperSecret1"), CLIENT_IP));
        String wrongPasswordMessage = catchMessage(() -> authService.login(loginRequest("owner@shop.com", "WrongPassword"), CLIENT_IP));

        assertThat(unknownEmailMessage).isEqualTo(wrongPasswordMessage);
    }

    // ---------- rate limiting ----------

    @Test
    @DisplayName("ล็อกอินผิดครบ 5 ครั้ง ครั้งถัดไปต้องถูกล็อกชั่วคราว แม้จะใส่รหัสถูก")
    void locksAccountAfterFiveFailedLogins() {
        when(appUserRepository.findByEmail("owner@shop.com"))
                .thenReturn(Optional.of(existingUser("owner@shop.com", "SuperSecret1", Roles.STORE_MANAGER)));

        for (int attempt = 1; attempt <= 5; attempt++) {
            assertThatThrownBy(() -> authService.login(loginRequest("owner@shop.com", "WrongPassword"), CLIENT_IP))
                    .as("ครั้งที่ %d ควรเป็นรหัสผ่านผิดธรรมดา", attempt)
                    .isInstanceOf(InvalidCredentialsException.class);
        }

        assertThatThrownBy(() -> authService.login(loginRequest("owner@shop.com", "SuperSecret1"), CLIENT_IP))
                .isInstanceOf(TooManyAttemptsException.class);
    }

    @Test
    @DisplayName("ล็อกอินสำเร็จต้องล้างตัวนับ ผู้ใช้ที่พิมพ์ผิดบ้างจึงไม่โดนล็อกสะสม")
    void successfulLoginResetsFailureCounter() {
        when(appUserRepository.findByEmail("owner@shop.com"))
                .thenReturn(Optional.of(existingUser("owner@shop.com", "SuperSecret1", Roles.STORE_MANAGER)));
        when(jwtUtil.generateToken(anyString(), anyString(), anyString())).thenReturn("token");

        for (int attempt = 1; attempt <= 4; attempt++) {
            assertThatThrownBy(() -> authService.login(loginRequest("owner@shop.com", "WrongPassword"), CLIENT_IP))
                    .isInstanceOf(InvalidCredentialsException.class);
        }

        authService.login(loginRequest("owner@shop.com", "SuperSecret1"), CLIENT_IP);

        // ถ้าตัวนับถูกล้างจริง 4 ครั้งถัดไปต้องยังไม่ถึงเพดาน
        for (int attempt = 1; attempt <= 4; attempt++) {
            assertThatThrownBy(() -> authService.login(loginRequest("owner@shop.com", "WrongPassword"), CLIENT_IP))
                    .isInstanceOf(InvalidCredentialsException.class);
        }
    }

    @Test
    @DisplayName("สมัครจาก IP เดิมเกินโควตาต้องถูกปฏิเสธ")
    void limitsRegistrationsFromSameIp() {
        when(appUserRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        for (int i = 1; i <= 5; i++) {
            authService.register(registerRequest("shop" + i + "@test.com", "SuperSecret1", "ร้าน " + i), CLIENT_IP);
        }

        assertThatThrownBy(() -> authService.register(registerRequest("shop6@test.com", "SuperSecret1", "ร้าน 6"), CLIENT_IP))
                .isInstanceOf(TooManyAttemptsException.class);
    }

    @Test
    @DisplayName("IP อื่นต้องไม่ถูกล็อกตามไปด้วยเมื่อ IP หนึ่งยิงจนเต็มโควตา")
    void limitIsScopedPerIp() {
        when(appUserRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        for (int i = 1; i <= 5; i++) {
            authService.register(registerRequest("shop" + i + "@test.com", "SuperSecret1", "ร้าน " + i), CLIENT_IP);
        }

        assertThat(authService.register(registerRequest("other@test.com", "SuperSecret1", "อีกร้าน"), "198.51.100.7"))
                .isNotNull();
    }

    // ---------- invite code ----------

    @Test
    @DisplayName("สมัครโดยไม่มี invite code หรือ code ผิดต้องถูกปฏิเสธและไม่บันทึก")
    void registerRejectsMissingOrWrongInviteCode() {
        assertThatThrownBy(() -> authService.register(registerRequest("a@shop.com", "SuperSecret1", "ร้าน", null), CLIENT_IP))
                .isInstanceOf(InvalidInviteCodeException.class);
        assertThatThrownBy(() -> authService.register(registerRequest("a@shop.com", "SuperSecret1", "ร้าน", "WRONG"), CLIENT_IP))
                .isInstanceOf(InvalidInviteCodeException.class);

        verify(appUserRepository, never()).saveAndFlush(any());
        verify(appUserRepository, never()).findByEmail(anyString());
    }

    @Test
    @DisplayName("invite code ผิดต้องถูกนับในโควตาสมัครต่อ IP")
    void wrongInviteCodeCountsTowardRegisterLimit() {
        for (int i = 1; i <= 5; i++) {
            assertThatThrownBy(() -> authService.register(registerRequest("a@shop.com", "SuperSecret1", "ร้าน", "WRONG"), CLIENT_IP))
                    .isInstanceOf(InvalidInviteCodeException.class);
        }

        assertThatThrownBy(() -> authService.register(registerRequest("a@shop.com", "SuperSecret1", "ร้าน"), CLIENT_IP))
                .isInstanceOf(TooManyAttemptsException.class);
    }

    @Test
    @DisplayName("ถ้าไม่ได้ตั้ง invite code ฝั่ง server ต้องปฏิเสธทุกคำขอ แม้ส่งค่าว่างมา")
    void blankServerInviteCodeRejectsEverything() {
        AuthService unconfigured = new AuthService(appUserRepository, passwordEncoder, jwtUtil, throttleService, "");

        assertThatThrownBy(() -> unconfigured.register(registerRequest("a@shop.com", "SuperSecret1", "ร้าน", ""), CLIENT_IP))
                .isInstanceOf(InvalidInviteCodeException.class);
        assertThatThrownBy(() -> unconfigured.resetPassword(resetRequest("a@shop.com", "", "NewSecret99"), CLIENT_IP))
                .isInstanceOf(InvalidInviteCodeException.class);
    }

    // ---------- reset password ----------

    @Test
    @DisplayName("รีเซ็ตด้วย invite code ถูกต้องต้องเก็บรหัสใหม่เป็น BCrypt hash")
    void resetPasswordUpdatesHash() {
        AppUser user = existingUser("owner@shop.com", "OldSecret1", Roles.STORE_MANAGER);
        when(appUserRepository.findByEmail("owner@shop.com")).thenReturn(Optional.of(user));

        authService.resetPassword(resetRequest("Owner@Shop.com", INVITE_CODE, "NewSecret99"), CLIENT_IP);

        verify(appUserRepository).save(user);
        assertThat(user.getPasswordHash()).startsWith("$2a$");
        assertThat(passwordEncoder.matches("NewSecret99", user.getPasswordHash())).isTrue();
        assertThat(passwordEncoder.matches("OldSecret1", user.getPasswordHash())).isFalse();
    }

    @Test
    @DisplayName("รีเซ็ตด้วย invite code ผิดต้องถูกปฏิเสธโดยไม่แตะฐานข้อมูล")
    void resetPasswordRejectsWrongInviteCode() {
        assertThatThrownBy(() -> authService.resetPassword(resetRequest("owner@shop.com", "WRONG", "NewSecret99"), CLIENT_IP))
                .isInstanceOf(InvalidInviteCodeException.class);

        verify(appUserRepository, never()).findByEmail(anyString());
        verify(appUserRepository, never()).save(any());
    }

    @Test
    @DisplayName("รีเซ็ตอีเมลที่ไม่มีบัญชีต้องไม่ error ผู้เรียกจึงแยกไม่ออกว่ามีบัญชีหรือไม่")
    void resetPasswordForUnknownEmailIsSilent() {
        when(appUserRepository.findByEmail("ghost@shop.com")).thenReturn(Optional.empty());

        authService.resetPassword(resetRequest("ghost@shop.com", INVITE_CODE, "NewSecret99"), CLIENT_IP);

        verify(appUserRepository, never()).save(any());
    }

    @Test
    @DisplayName("รีเซ็ตอีเมลเดิมเกินโควตาต้องถูกล็อกชั่วคราว")
    void resetPasswordIsRateLimitedPerEmail() {
        for (int i = 1; i <= 5; i++) {
            assertThatThrownBy(() -> authService.resetPassword(resetRequest("owner@shop.com", "WRONG", "NewSecret99"), CLIENT_IP))
                    .isInstanceOf(InvalidInviteCodeException.class);
        }

        // แม้เปลี่ยน IP แล้วใส่ code ถูก ก็ยังถูกล็อกตามอีเมล
        assertThatThrownBy(() -> authService.resetPassword(resetRequest("owner@shop.com", INVITE_CODE, "NewSecret99"), "198.51.100.7"))
                .isInstanceOf(TooManyAttemptsException.class);
    }

    @Test
    @DisplayName("รีเซ็ตจาก IP เดิมเกินโควตาต้องถูกล็อก แม้จะสลับอีเมลไปเรื่อยๆ")
    void resetPasswordIsRateLimitedPerIp() {
        for (int i = 1; i <= 10; i++) {
            String email = "shop" + i + "@test.com";
            assertThatThrownBy(() -> authService.resetPassword(resetRequest(email, "WRONG", "NewSecret99"), CLIENT_IP))
                    .isInstanceOf(InvalidInviteCodeException.class);
        }

        assertThatThrownBy(() -> authService.resetPassword(resetRequest("fresh@test.com", INVITE_CODE, "NewSecret99"), CLIENT_IP))
                .isInstanceOf(TooManyAttemptsException.class);
    }

    private String catchMessage(Runnable action) {
        try {
            action.run();
            throw new AssertionError("คาดว่าจะเกิด InvalidCredentialsException แต่ไม่เกิด");
        } catch (InvalidCredentialsException ex) {
            return ex.getMessage();
        }
    }
}
