package com.smartsell.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * รหัส OTP สำหรับยืนยันอีเมลหรือตั้งรหัสผ่านใหม่ (ดู purpose)
 *
 * หนึ่งบัญชีมีได้แค่หนึ่งแถว ขอรหัสใหม่เมื่อไหร่ ไม่ว่างานไหน แถวเดิมจะถูกลบทิ้ง
 * cooldown 60 วินาทีจึงนับรวมทั้งสองงาน กันใช้งานหนึ่งเป็นช่องยิงเมลข้ามงานอีกงาน
 *
 * เก็บแค่ hash ของรหัส (BCrypt) ไม่เก็บตัวเลขจริง คนที่เข้าถึงฐานข้อมูลได้จึงอ่านรหัสไปใช้ไม่ได้
 */
@Entity
@Table(name = "email_otp")
public class EmailOtp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private AppUser user;

    // DEFAULT มีไว้ให้ ddl-auto=update เติมคอลัมน์นี้ให้แถวที่มีอยู่ก่อนได้
    @Enumerated(EnumType.STRING)
    @Column(name = "purpose", nullable = false, length = 20,
            columnDefinition = "VARCHAR(20) NOT NULL DEFAULT 'VERIFY_EMAIL'")
    private OtpPurpose purpose;

    @Column(name = "code_hash", nullable = false, length = 255)
    private String codeHash;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "failed_attempts", nullable = false)
    private int failedAttempts = 0;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public Long getId() { return id; }

    public AppUser getUser() { return user; }
    public void setUser(AppUser user) { this.user = user; }

    public OtpPurpose getPurpose() { return purpose; }
    public void setPurpose(OtpPurpose purpose) { this.purpose = purpose; }

    public String getCodeHash() { return codeHash; }
    public void setCodeHash(String codeHash) { this.codeHash = codeHash; }

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }

    public int getFailedAttempts() { return failedAttempts; }
    public void setFailedAttempts(int failedAttempts) { this.failedAttempts = failedAttempts; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
