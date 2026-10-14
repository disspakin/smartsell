package com.smartsell.controller;

import com.smartsell.exception.InvalidRequestException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

/**
 * ช่วงเวลาของ Dashboard จาก ?from=YYYY-MM-DD&to=YYYY-MM-DD (รวมทั้งสองวัน)
 *
 * แปลงเป็น [start, end) ให้ query — end คือเที่ยงคืนต้นวันถัดจาก to จึงนับครบถึง 23:59 ของวันสุดท้าย
 * ไม่ส่งตัวไหนมา = ไม่จำกัดด้านนั้น ("ทั้งหมด" คือไม่ส่งทั้งคู่)
 *
 * created_at เก็บเป็นเวลาท้องถิ่นของเครื่องที่รัน backend (LocalDateTime.now())
 * วันที่ที่รับมาจึงเทียบกับเวลาเดียวกันนั้นตรงๆ — เครื่องตั้งเวลาไทยไว้ ก็คือเวลาไทย
 */
record DashboardPeriod(LocalDateTime start, LocalDateTime end) {

    // ไกลพอจะครอบทุกแถว และอยู่ในช่วงที่ทั้ง PostgreSQL และ H2 รับได้
    static final LocalDateTime EARLIEST = LocalDateTime.of(1970, 1, 1, 0, 0);
    static final LocalDateTime LATEST = LocalDateTime.of(9999, 1, 1, 0, 0);

    static DashboardPeriod of(String from, String to) {
        LocalDate fromDate = parse(from, "from");
        LocalDate toDate = parse(to, "to");
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new InvalidRequestException("วันเริ่มต้น (" + fromDate + ") ต้องไม่อยู่หลังวันสิ้นสุด (" + toDate + ")");
        }
        return new DashboardPeriod(
                fromDate == null ? EARLIEST : fromDate.atStartOfDay(),
                toDate == null ? LATEST : toDate.plusDays(1).atStartOfDay());
    }

    private static LocalDate parse(String value, String name) {
        if (value == null || value.isBlank()) return null;
        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException e) {
            throw new InvalidRequestException("รูปแบบวันที่ " + name + " ไม่ถูกต้อง ต้องเป็น YYYY-MM-DD: \"" + value + "\"");
        }
    }
}
