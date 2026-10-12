
package com.smartsell.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "app_user")
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, length = 255)
    private String email;

    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    @Column(nullable = false, length = 20)
    private String role = "STORE_MANAGER"; // STORE_MANAGER | ADMIN

    @Column(name = "display_name", length = 100)
    private String displayName;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    /**
     * บัญชีที่สมัครใหม่เริ่มที่ false จนกว่าจะกรอก OTP ถูก (ตั้งค่าที่ AuthService.register)
     *
     * DEFAULT TRUE ใน columnDefinition มีไว้ให้ ddl-auto=update เติมคอลัมน์นี้ให้บัญชีที่มีอยู่ก่อน
     * ระบบ OTP เป็น true ทั้งหมด ไม่งั้นทุกคนที่สมัครไว้แล้วจะล็อกอินไม่ได้ทันทีที่อัปเดต
     */
    @Column(name = "email_verified", nullable = false, columnDefinition = "BOOLEAN NOT NULL DEFAULT TRUE")
    private boolean emailVerified = false;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    public boolean isEmailVerified() { return emailVerified; }
    public void setEmailVerified(boolean emailVerified) { this.emailVerified = emailVerified; }
}
