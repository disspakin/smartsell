package com.smartsell.dto;

import java.math.BigDecimal;
import java.util.List;

public class LuckyColorDTO {

    // sent from the frontend after the customer picks a birth day + a goal
    // gender: MALE / FEMALE / ANY หรือ null (ไม่ระบุ) — กรองสินค้าแบบเดียวกับผู้ช่วย AI (ดู TargetGroup)
    public record Request(String sessionToken, String birthWeekday, String luckyGoal, String gender) {}

    public record Result(String color, List<String> colorHexes, String reasoning, List<LuckyProduct> products) {}

    /**
     * สินค้าที่แนะนำ "พร้อมสีที่ใช่" — สินค้าหลายสีอย่างโปโลแนะนำได้ทุกวัน แต่คนละสี
     * imageUrl เป็นรูปของสีนั้น (ถ้ามี) และหน้าเว็บใช้ luckyColor เลือกสีให้ในหน้าสินค้า
     */
    public record LuckyProduct(Long id, String name, BigDecimal price, String imageUrl,
                               String luckyColor, String luckyColorHex) {}
}
