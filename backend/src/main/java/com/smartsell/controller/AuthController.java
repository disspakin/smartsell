package com.smartsell.controller;

import com.smartsell.dto.AuthResponse;
import com.smartsell.dto.LoginRequest;
import com.smartsell.dto.RegisterRequest;
import com.smartsell.dto.ResetPasswordRequest;
import com.smartsell.dto.UserProfileResponse;
import com.smartsell.entity.AppUser;
import com.smartsell.service.AuthService;
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

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * สมัครบัญชี — ตอบกลับเป็นข้อมูลผู้ใช้ ไม่มี token
     * ผู้สมัครต้องไปล็อกอินเองที่ /api/auth/login อีกครั้ง
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

    /**
     * ตั้งรหัสผ่านใหม่ด้วย invite code — ตอบข้อความเดียวกันเสมอเมื่อ code ถูก
     * ไม่ว่าอีเมลจะมีบัญชีหรือไม่ (ดูเหตุผลที่ AuthService.resetPassword)
     */
    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@Valid @RequestBody ResetPasswordRequest request,
                                                             HttpServletRequest httpRequest) {
        authService.resetPassword(request, clientIpOf(httpRequest));
        return ResponseEntity.ok(Map.of("message",
                "หากอีเมลนี้มีบัญชีอยู่ รหัสผ่านได้ถูกเปลี่ยนแล้ว กรุณาเข้าสู่ระบบด้วยรหัสผ่านใหม่"));
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
