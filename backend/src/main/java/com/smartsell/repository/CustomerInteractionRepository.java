package com.smartsell.repository;

import com.smartsell.entity.CustomerInteraction;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

/**
 * query ของ Dashboard ทุกตัวรับช่วงเวลา [start, end) — start รวม end ไม่รวม
 * เลือก "ทั้งหมด" ใช้ช่วงกว้างมาก (ดู DashboardPeriod) ไม่ต้องมี query ชุดที่สอง
 */
public interface CustomerInteractionRepository extends JpaRepository<CustomerInteraction, Long> {

    String IN_RANGE = " ci.createdAt >= :start AND ci.createdAt < :end ";

    @Query("SELECT COUNT(ci) FROM CustomerInteraction ci WHERE ci.eventType = :eventType AND" + IN_RANGE)
    long countByEventTypeBetween(String eventType, LocalDateTime start, LocalDateTime end);

    boolean existsBySessionIdAndProductIdAndEventType(Long sessionId, Long productId, String eventType);

    boolean existsByClientTokenAndEventTypeAndLuckyColor(String clientToken, String eventType, String luckyColor);

    @Query("SELECT ci.personalColor, COUNT(ci) FROM CustomerInteraction ci WHERE ci.personalColor IS NOT NULL AND" + IN_RANGE + "GROUP BY ci.personalColor")
    List<Object[]> countByPersonalColorGroup(LocalDateTime start, LocalDateTime end);

    @Query("SELECT ci.occasion, COUNT(ci) FROM CustomerInteraction ci WHERE ci.occasion IS NOT NULL AND" + IN_RANGE + "GROUP BY ci.occasion")
    List<Object[]> countByOccasionGroup(LocalDateTime start, LocalDateTime end);

    @Query("SELECT ci.luckyColor, COUNT(ci) FROM CustomerInteraction ci WHERE ci.luckyColor IS NOT NULL AND" + IN_RANGE + "GROUP BY ci.luckyColor")
    List<Object[]> countByLuckyColorGroup(LocalDateTime start, LocalDateTime end);

    @Query("SELECT ci.size, COUNT(ci) FROM CustomerInteraction ci WHERE ci.size IS NOT NULL AND" + IN_RANGE + "GROUP BY ci.size")
    List<Object[]> countBySizeGroup(LocalDateTime start, LocalDateTime end);

    // เงื่อนไขช่วงเวลาต้องอยู่ใน ON ไม่ใช่ WHERE — ไม่งั้นสินค้าที่ไม่มีใครดูในช่วงนั้นจะหายไปจากตาราง
    @Query("SELECT p.id, p.name, p.imageUrl, p.price, " +
           "SUM(CASE WHEN ci.eventType = 'VIEW' THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN ci.eventType = 'INTERESTED_CLICK' THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN ci.eventType = 'AI_RECOMMENDED' THEN 1 ELSE 0 END) " +
           "FROM Product p LEFT JOIN CustomerInteraction ci ON p.id = ci.product.id AND" + IN_RANGE +
           "GROUP BY p.id, p.name, p.imageUrl, p.price")
    List<Object[]> findAllProductAnalytics(LocalDateTime start, LocalDateTime end);

    @Query("SELECT p.id, p.name, p.imageUrl, p.price, COUNT(ci) FROM CustomerInteraction ci JOIN ci.product p WHERE ci.eventType = :eventType AND" + IN_RANGE + "GROUP BY p.id, p.name, p.imageUrl, p.price ORDER BY COUNT(ci) DESC")
    List<Object[]> findTopProductsByEventType(String eventType, LocalDateTime start, LocalDateTime end);

    // สินค้ายอดนิยมหน้าแรก: เรียงตามจำนวนคนเปิดดู (VIEW) แล้วตามจำนวนกดถูกใจ (INTERESTED_CLICK)
    // LEFT JOIN ให้สินค้าที่ยังไม่มีใครดูต่อท้าย หน้าแรกจะได้ครบจำนวนเสมอ
    @Query("SELECT p.id FROM Product p LEFT JOIN CustomerInteraction ci ON p.id = ci.product.id " +
           "GROUP BY p.id " +
           "ORDER BY SUM(CASE WHEN ci.eventType = 'VIEW' THEN 1 ELSE 0 END) DESC, " +
           "SUM(CASE WHEN ci.eventType = 'INTERESTED_CLICK' THEN 1 ELSE 0 END) DESC, p.id")
    List<Long> findProductIdsByPopularity(Pageable pageable);

    @Query("SELECT AVG(ci.rating) FROM CustomerInteraction ci WHERE ci.rating IS NOT NULL AND" + IN_RANGE)
    Double findAverageRating(LocalDateTime start, LocalDateTime end);

    @Query("SELECT MIN(ci.createdAt) FROM CustomerInteraction ci")
    LocalDateTime findFirstCreatedAt();
}
