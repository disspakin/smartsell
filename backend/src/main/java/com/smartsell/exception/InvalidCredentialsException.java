package com.smartsell.exception;

/**
 * อีเมลหรือรหัสผ่านไม่ถูกต้อง — map เป็น HTTP 401 Unauthorized
 *
 * ใช้ข้อความเดียวกันทั้งกรณี "ไม่พบอีเมล" และ "รหัสผ่านผิด" โดยตั้งใจ
 * ถ้าแยกข้อความ ผู้โจมตีจะใช้หน้าล็อกอินไล่เดาได้ว่าอีเมลไหนมีบัญชีอยู่จริง (user enumeration)
 */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException(String message) {
        super(message);
    }
}
