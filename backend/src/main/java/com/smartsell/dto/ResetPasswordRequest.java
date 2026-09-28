package com.smartsell.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** คำขอตั้งรหัสผ่านใหม่โดยยืนยันด้วย invite code แทนอีเมล */
public class ResetPasswordRequest {

    @NotBlank(message = "กรุณากรอกอีเมล")
    @Email(message = "รูปแบบอีเมลไม่ถูกต้อง")
    @Size(max = 255, message = "อีเมลยาวเกิน 255 ตัวอักษร")
    private String email;

    // เหมือน RegisterRequest: ไม่ใส่ @NotBlank เพื่อให้กรณีไม่ส่งมาได้ 403 และถูกนับใน rate limit
    @Size(max = 200, message = "invite code ยาวเกินไป")
    private String inviteCode;

    // กฎเดียวกับตอนสมัคร — BCrypt ตัดข้อความทิ้งหลังไบต์ที่ 72
    @NotBlank(message = "กรุณากรอกรหัสผ่านใหม่")
    @Size(min = 8, max = 72, message = "รหัสผ่านต้องยาว 8-72 ตัวอักษร")
    private String newPassword;

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getInviteCode() { return inviteCode; }
    public void setInviteCode(String inviteCode) { this.inviteCode = inviteCode; }

    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
}