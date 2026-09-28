package com.smartsell.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * คำขอสมัครบัญชีพนักงาน/เจ้าของร้าน
 *
 * ไม่มีฟิลด์ role โดยตั้งใจ — role ถูกกำหนดที่ AuthService ฝั่ง server เท่านั้น
 * เพื่อไม่ให้ผู้เรียก API ยกระดับสิทธิ์ตัวเองด้วยการแนบ role มาใน request body
 */
public class RegisterRequest {

    @NotBlank(message = "กรุณากรอกอีเมล")
    @Email(message = "รูปแบบอีเมลไม่ถูกต้อง")
    @Size(max = 255, message = "อีเมลยาวเกิน 255 ตัวอักษร")
    private String email;

    // BCrypt ตัดข้อความทิ้งหลังไบต์ที่ 72 จึงกันไว้ไม่ให้เกิน
    @NotBlank(message = "กรุณากรอกรหัสผ่าน")
    @Size(min = 8, max = 72, message = "รหัสผ่านต้องยาว 8-72 ตัวอักษร")
    private String password;

    @NotBlank(message = "กรุณากรอกชื่อที่ใช้แสดง")
    @Size(max = 100, message = "ชื่อที่ใช้แสดงยาวเกิน 100 ตัวอักษร")
    private String displayName;

    // ไม่ใส่ @NotBlank โดยตั้งใจ — ถ้าไม่ส่งมาต้องได้ 403 จาก AuthService
    // ไม่ใช่ 400 จาก validation และต้องถูกนับใน rate limit เหมือนส่งรหัสผิด
    @Size(max = 200, message = "invite code ยาวเกินไป")
    private String inviteCode;

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getInviteCode() { return inviteCode; }
    public void setInviteCode(String inviteCode) { this.inviteCode = inviteCode; }
}
