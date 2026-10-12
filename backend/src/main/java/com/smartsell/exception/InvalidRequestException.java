package com.smartsell.exception;

/** ค่าที่ส่งมาไม่อยู่ในตัวเลือกที่ระบบรู้จัก (เช่น วันเกิดที่ไม่มีจริง) — map เป็น HTTP 400 Bad Request */
public class InvalidRequestException extends RuntimeException {

    public InvalidRequestException(String message) {
        super(message);
    }
}
