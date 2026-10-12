package com.smartsell.repository;

import com.smartsell.entity.EmailOtp;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface EmailOtpRepository extends JpaRepository<EmailOtp, Long> {

    Optional<EmailOtp> findByUserId(Long userId);

    /**
     * ล็อกแถวไว้ระหว่างตรวจรหัส — ไม่งั้นยิง verify พร้อมกันหลายคำขอ
     * ทุกคำขอจะอ่าน failedAttempts ค่าเดิมได้ และเดาเกิน 5 ครั้งได้
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM EmailOtp o WHERE o.user.id = :userId")
    Optional<EmailOtp> findByUserIdForUpdate(Long userId);

    @Modifying
    @Query("DELETE FROM EmailOtp o WHERE o.user.id = :userId")
    void deleteByUserId(Long userId);
}
