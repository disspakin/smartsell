package com.smartsell.repository;

import com.smartsell.entity.CustomerSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;

public interface CustomerSessionRepository extends JpaRepository<CustomerSession, Long> {

    /** จำนวน session ในช่วง [start, end) — ใช้กับการ์ด Total Sessions บน Dashboard */
    @Query("SELECT COUNT(cs) FROM CustomerSession cs WHERE cs.createdAt >= :start AND cs.createdAt < :end")
    long countCreatedBetween(LocalDateTime start, LocalDateTime end);

    @Query("SELECT MIN(cs.createdAt) FROM CustomerSession cs")
    LocalDateTime findFirstCreatedAt();
}
