package com.smartsell.exception;

/** invite code ไม่ถูกต้องหรือไม่ได้ส่งมา — map เป็น HTTP 403 Forbidden */
public class InvalidInviteCodeException extends RuntimeException {

    public InvalidInviteCodeException(String message) {
        super(message);
    }
}