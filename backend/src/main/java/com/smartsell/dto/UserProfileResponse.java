package com.smartsell.dto;

import com.smartsell.entity.AppUser;

/**
 * ข้อมูลผู้ใช้ที่ส่งกลับให้ client — ไม่มี passwordHash โดยตั้งใจ
 */
public class UserProfileResponse {

    private final Long id;
    private final String email;
    private final String displayName;
    private final String role;

    public UserProfileResponse(AppUser user) {
        this.id = user.getId();
        this.email = user.getEmail();
        this.displayName = user.getDisplayName();
        this.role = user.getRole();
    }

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getDisplayName() { return displayName; }
    public String getRole() { return role; }
}
