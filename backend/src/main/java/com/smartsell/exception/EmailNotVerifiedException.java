package com.smartsell.exception;

/**
 * รหัสผ่านถูกแต่ยังไม่ได้ยืนยันอีเมล — map เป็น HTTP 403 พร้อม code EMAIL_NOT_VERIFIED
 * หน้าเว็บใช้ code นี้พาผู้ใช้ไปหน้ากรอก OTP แทนการแสดง error เฉยๆ
 */
public class EmailNotVerifiedException extends RuntimeException {

    public static final String CODE = "EMAIL_NOT_VERIFIED";

    public EmailNotVerifiedException(String message) {
        super(message);
    }
}
