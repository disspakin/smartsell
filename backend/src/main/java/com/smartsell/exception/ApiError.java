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
 *
 * code เป็นตัวระบุชนิด error ที่หน้าเว็บใช้ตัดสินใจต่อ (เช่น EMAIL_NOT_VERIFIED)
 * ใส่มาเฉพาะกรณีที่ status อย่างเดียวแยกไม่ออก
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiError {

    private final LocalDateTime timestamp = LocalDateTime.now();
    private final int status;
    private final String error;
    private final Map<String, String> fieldErrors;
    private final String code;

    public ApiError(int status, String error) {
        this(status, error, null, null);
    }

    public ApiError(int status, String error, Map<String, String> fieldErrors) {
        this(status, error, fieldErrors, null);
    }

    public ApiError(int status, String error, Map<String, String> fieldErrors, String code) {
        this.status = status;
        this.error = error;
        this.fieldErrors = fieldErrors;
        this.code = code;
    }

    public LocalDateTime getTimestamp() { return timestamp; }
    public int getStatus() { return status; }
    public String getError() { return error; }
    public Map<String, String> getFieldErrors() { return fieldErrors; }
    public String getCode() { return code; }
}
