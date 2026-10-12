package com.smartsell.dto;

import java.util.List;

// what GET /api/store returns — footer หน้าเว็บ: ข้อมูลร้านทุกแถว, เวลาเปิด 7 วัน (จันทร์ก่อน), สถานะ ณ ตอนนี้ (เวลาไทย)
public record StoreInfoDTO(List<Item> info, List<Day> hours, boolean openNow, String statusText) {

    public record Item(String key, String label, String value) {}

    // openTime/closeTime เป็น "HH:mm" หรือ null ถ้าวันนั้นปิด
    public record Day(int dayOfWeek, String dayName, String openTime, String closeTime, boolean closed) {}
}
