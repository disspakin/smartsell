package com.smartsell.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** ขั้นแรกของการตั้งรหัสผ่านใหม่ — ขอให้ส่งรหัส OTP ไปที่อีเมล */
public class ForgotPasswordRequest {

    @NotBlank(message = "กรุณากรอกอีเมล")
    @Email(message = "รูปแบบอีเมลไม่ถูกต้อง")
    @Size(max = 255, message = "อีเมลยาวเกิน 255 ตัวอักษร")
    private String email;

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
