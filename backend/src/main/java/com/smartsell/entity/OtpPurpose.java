package com.smartsell.entity;

/**
 * รหัส OTP ออกให้ใช้กับงานไหน — รหัสยืนยันอีเมลเอาไปตั้งรหัสผ่านใหม่ไม่ได้ และกลับกัน
 */
public enum OtpPurpose {
    VERIFY_EMAIL,
    RESET_PASSWORD
}
