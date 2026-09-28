package com.smartsell.security;

/**
 * บทบาทผู้ใช้ เก็บเป็น String ในคอลัมน์ app_user.role
 *
 * Spring Security เติม prefix "ROLE_" ให้เองเมื่อใช้ hasRole()/hasAnyRole()
 * ค่าที่เก็บใน DB จึงต้องไม่มี prefix — JwtAuthFilter เป็นคนเติมตอนสร้าง authority
 */
public final class Roles {

    /** ผู้ดูแลร้าน — เข้าถึง /api/admin/** ได้ เป็นบทบาทที่ได้จากการสมัครผ่าน hidden route */
    public static final String STORE_MANAGER = "STORE_MANAGER";

    /** ผู้ดูแลระบบ — สิทธิ์ครอบคลุม STORE_MANAGER */
    public static final String ADMIN = "ADMIN";

    private Roles() {
    }
}
