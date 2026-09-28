package com.smartsell.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * รูปแบบ error เดียวที่ใช้ทั้งระบบ เพื่อให้ฝั่ง client แกะได้ด้วยโค้ดชุดเดียว
 *
 * {
 *   "timestamp": "2026-09-27T14:32:10",
 *   "status": 400,
 *   "error": "ข้อมูลที่กรอกไม่ถูกต้อง",
 *   "fieldErrors": { "email": "รูปแบบอีเมลไม่ถูกต้อง" }
 * }
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiError {

    private final LocalDateTime timestamp = LocalDateTime.now();
    private final int status;
    private final String error;
    private final Map<String, String> fieldErrors;

    public ApiError(int status, String error) {
        this(status, error, null);
    }

    public ApiError(int status, String error, Map<String, String> fieldErrors) {
        this.status = status;
        this.error = error;
        this.fieldErrors = fieldErrors;
    }

    public LocalDateTime getTimestamp() { return timestamp; }
    public int getStatus() { return status; }
    public String getError() { return error; }
    public Map<String, String> getFieldErrors() { return fieldErrors; }
}
