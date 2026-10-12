package com.smartsell.exception;

/** รหัส OTP ผิด หมดอายุ หรือถูกปิดเพราะกรอกผิดครบกำหนด — map เป็น HTTP 400 Bad Request */
public class InvalidOtpException extends RuntimeException {

    public InvalidOtpException(String message) {
        super(message);
    }
}
