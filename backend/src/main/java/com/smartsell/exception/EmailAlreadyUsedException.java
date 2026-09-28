package com.smartsell.exception;

/** อีเมลนี้มีบัญชีอยู่แล้ว — map เป็น HTTP 409 Conflict */
public class EmailAlreadyUsedException extends RuntimeException {

    public EmailAlreadyUsedException(String message) {
        super(message);
    }
}
