package com.smartsell.controller;

import com.smartsell.dto.AuthResponse;
import com.smartsell.dto.ForgotPasswordRequest;
import com.smartsell.dto.LoginRequest;
import com.smartsell.dto.RegisterRequest;
import com.smartsell.dto.ResendOtpRequest;
import com.smartsell.dto.ResetPasswordRequest;
import com.smartsell.dto.UserProfileResponse;
import com.smartsell.dto.VerifyOtpRequest;
import com.smartsell.entity.AppUser;
import com.smartsell.service.AuthService;
import com.smartsell.service.EmailOtpService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final EmailOtpService otpService;

    public AuthController(AuthService authService, EmailOtpService otpService) {
        this.authService = authService;
        this.otpService = otpService;
    }

    /**
     * สมัครบัญชี — ตอบกลับเป็นข้อมูลผู้ใช้ ไม่มี token และส่ง OTP ไปที่อีเมล
     * ผู้สมัครต้องยืนยันอีเมลที่ /api/auth/verify-otp แล้วค่อยล็อกอินที่ /api/auth/login
     */
    @PostMapping("/register")
    public ResponseEntity<UserProfileResponse> register(@Valid @RequestBody RegisterRequest request,
                                                        HttpServletRequest httpRequest) {
        AppUser created = authService.register(request, clientIpOf(httpRequest));
        return ResponseEntity.status(HttpStatus.CREATED).body(new UserProfileResponse(created));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request,
                                              HttpServletRequest httpRequest) {
        return ResponseEntity.ok(authService.login(request, clientIpOf(httpRequest)));
    }

    /** ยืนยันอีเมลด้วยรหัส OTP 6 หลัก — สำเร็จแล้วผู้ใช้ต้องไปล็อกอินเอง ไม่ออก token ให้ */
    @PostMapping("/verify-otp")
    public ResponseEntity<Map<String, String>> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        otpService.verify(request.getEmail(), request.getCode());
        return ResponseEntity.ok(Map.of("message", "ยืนยันอีเมลสำเร็จแล้ว กรุณาเข้าสู่ระบบ"));
    }

    /**
     * ขอรหัส OTP ใหม่ — ตอบข้อความเดียวกันไม่ว่าอีเมลจะมีบัญชีหรือไม่
     * ขอถี่กว่า 60 วินาทีได้ 429 พร้อม Retry-After ให้หน้าเว็บใช้นับถอยหลัง
     */
    @PostMapping("/resend-otp")
    public ResponseEntity<Map<String, String>> resendOtp(@Valid @RequestBody ResendOtpRequest request) {
        otpService.resend(request.getEmail());
        return ResponseEntity.ok(Map.of("message",
                "หากอีเมลนี้มีบัญชีที่ยังไม่ได้ยืนยัน เราได้ส่งรหัสใหม่ไปแล้ว"));
    }

    /**
     * ลืมรหัสผ่าน ขั้นที่ 1: ส่ง OTP ไปที่อีเมล — ตอบข้อความเดียวกันเสมอ
     * ไม่ว่าอีเมลจะมีบัญชีหรือไม่ (ดูเหตุผลที่ AuthService.requestPasswordReset)
     * ขอถี่กว่า 60 วินาทีได้ 429 พร้อม Retry-After ให้หน้าเว็บใช้นับถอยหลัง
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request,
                                                              HttpServletRequest httpRequest) {
        authService.requestPasswordReset(request.getEmail(), clientIpOf(httpRequest));
        return ResponseEntity.ok(Map.of("message",
                "หากอีเมลนี้มีบัญชีอยู่ เราได้ส่งรหัสสำหรับตั้งรหัสผ่านใหม่ไปแล้ว"));
    }

    /** ลืมรหัสผ่าน ขั้นที่ 2: ตรวจ OTP แล้วตั้งรหัสผ่านใหม่ */
    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(Map.of("message", "ตั้งรหัสผ่านใหม่สำเร็จแล้ว กรุณาเข้าสู่ระบบด้วยรหัสผ่านใหม่"));
    }

    /**
     * ข้อมูลผู้ใช้ปัจจุบัน — ใช้ตรวจว่า token ยังใช้ได้อยู่ไหมก่อนเข้าหน้าที่ต้องล็อกอิน
     *
     * ไม่ต้องแกะ Authorization header เอง เพราะ JwtAuthFilter ตรวจ token
     * และใส่อีเมลไว้ใน SecurityContext ให้แล้ว ส่วนคำขอที่ไม่มี token จะถูก
     * SecurityConfig ปัดตกเป็น 401 ตั้งแต่ก่อนเข้าเมธอดนี้
     */
    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getCurrentUser(Authentication authentication) {
        String email = authentication.getName();
        return ResponseEntity.ok(new UserProfileResponse(authService.getUserByEmail(email)));
    }

    /**
     * IP ที่ใช้เป็นกุญแจนับจำนวนครั้งของ rate limit
     *
     * ใช้ getRemoteAddr() ไม่ใช่ X-Forwarded-For เพราะ header นั้นผู้เรียกปลอมได้เอง
     * ถ้าเชื่อมันจะกลายเป็นช่องข้าม rate limit ด้วยการสุ่ม IP ปลอมใส่ทุกคำขอ
     * ถ้าวันหนึ่งนำไป deploy หลัง reverse proxy ต้องตั้ง server.forward-headers-strategy
     * ให้ Spring จัดการแทน จึงจะอ่านค่าที่ proxy ใส่มาได้อย่างปลอดภัย
     */
    private String clientIpOf(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        return remoteAddr == null ? "unknown" : remoteAddr;
    }
}
