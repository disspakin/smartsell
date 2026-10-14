package com.smartsell.entity;

import java.util.Set;

/**
 * สินค้าทำมาสำหรับใคร (product.target_group) และเพศที่ลูกค้าเลือกในผู้ช่วย AI (customer_session.gender)
 *
 * เก็บเป็น String ใน DB แบบเดียวกับ AppUser.role — ค่าที่ไม่รู้จักจะไม่ทำให้อ่านแถวนั้นไม่ได้
 */
public final class TargetGroup {

    // ── สินค้า ──
    public static final String MALE = "MALE";
    public static final String FEMALE = "FEMALE";
    public static final String UNISEX = "UNISEX";
    /** เสื้อเด็ก — ผู้ช่วย AI ไม่แนะนำ เพราะถามไซส์ผู้ใหญ่ */
    public static final String KIDS = "KIDS";

    // ── คำตอบของลูกค้า: MALE / FEMALE หรือไม่ระบุ ──
    public static final String ANY = "ANY";

    private static final Set<String> CUSTOMER_GENDERS = Set.of(MALE, FEMALE, ANY);

    private TargetGroup() {}

    /** แปลงค่าที่ AI ส่งมาเป็นค่าที่ระบบรู้จัก — ค่าอื่นคืน null (ถือว่ายังไม่ได้ตอบ) */
    public static String normalizeCustomerGender(String value) {
        if (value == null) return null;
        String upper = value.trim().toUpperCase();
        return CUSTOMER_GENDERS.contains(upper) ? upper : null;
    }

    /**
     * สินค้านี้แนะนำให้ผู้ใหญ่ที่เลือกเพศนี้ได้ไหม
     * เสื้อเด็กไม่แนะนำเลย / ไม่ระบุเพศ (ANY หรือ null) ได้ทุกตัวยกเว้นเสื้อเด็ก / UNISEX ได้ทุกเพศ
     */
    public static boolean suitsAdult(String productGroup, String customerGender) {
        if (KIDS.equals(productGroup)) return false;
        if (customerGender == null || ANY.equals(customerGender)) return true;
        return productGroup == null || UNISEX.equals(productGroup) || productGroup.equals(customerGender);
    }

    /** ข้อความสำหรับบอก AI ในข้อมูลสินค้า */
    public static String thaiLabel(String productGroup) {
        if (productGroup == null) return "ทุกเพศ";
        return switch (productGroup) {
            case MALE -> "ผู้ชาย";
            case FEMALE -> "ผู้หญิง";
            case KIDS -> "เด็ก";
            default -> "ทุกเพศ";
        };
    }
}
