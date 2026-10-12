package com.smartsell.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** ขั้นที่สองของการตั้งรหัสผ่านใหม่ — ยืนยันด้วยรหัส OTP ที่ส่งไปทางอีเมล */
public class ResetPasswordRequest {

    @NotBlank(message = "กรุณากรอกอีเมล")
    @Email(message = "รูปแบบอีเมลไม่ถูกต้อง")
    @Size(max = 255, message = "อีเมลยาวเกิน 255 ตัวอักษร")
    private String email;

    @NotBlank(message = "กรุณากรอกรหัส OTP")
    @Pattern(regexp = "\\d{6}", message = "รหัส OTP ต้องเป็นตัวเลข 6 หลัก")
    private String code;

    // กฎเดียวกับตอนสมัคร — BCrypt ตัดข้อความทิ้งหลังไบต์ที่ 72
    @NotBlank(message = "กรุณากรอกรหัสผ่านใหม่")
    @Size(min = 8, max = 72, message = "รหัสผ่านต้องยาว 8-72 ตัวอักษร")
    private String newPassword;

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
}
