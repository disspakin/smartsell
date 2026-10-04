package com.smartsell.repository;

import com.smartsell.entity.CustomerInteraction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CustomerInteractionRepository extends JpaRepository<CustomerInteraction, Long> {

    long countByEventType(String eventType);

    @Query("SELECT ci.personalColor, COUNT(ci) FROM CustomerInteraction ci WHERE ci.personalColor IS NOT NULL GROUP BY ci.personalColor")
    List<Object[]> countByPersonalColorGroup();

    @Query("SELECT ci.occasion, COUNT(ci) FROM CustomerInteraction ci WHERE ci.occasion IS NOT NULL GROUP BY ci.occasion")
    List<Object[]> countByOccasionGroup();

    @Query("SELECT ci.luckyColor, COUNT(ci) FROM CustomerInteraction ci WHERE ci.luckyColor IS NOT NULL GROUP BY ci.luckyColor")
    List<Object[]> countByLuckyColorGroup();

    @Query("SELECT ci.size, COUNT(ci) FROM CustomerInteraction ci WHERE ci.size IS NOT NULL GROUP BY ci.size")
    List<Object[]> countBySizeGroup();

    @Query("SELECT p.id, p.name, p.imageUrl, p.price, " +
           "SUM(CASE WHEN ci.eventType = 'VIEW' THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN ci.eventType = 'INTERESTED_CLICK' THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN ci.eventType = 'AI_RECOMMENDED' THEN 1 ELSE 0 END) " +
           "FROM Product p LEFT JOIN CustomerInteraction ci ON p.id = ci.product.id " +
           "GROUP BY p.id, p.name, p.imageUrl, p.price")
    List<Object[]> findAllProductAnalytics();

    @Query("SELECT p.id, p.name, p.imageUrl, p.price, COUNT(ci) FROM CustomerInteraction ci JOIN ci.product p WHERE ci.eventType = 'INTERESTED_CLICK' GROUP BY p.id, p.name, p.imageUrl, p.price ORDER BY COUNT(ci) DESC")
    List<Object[]> findTopInterestedProducts();

    @Query("SELECT p.id, p.name, p.imageUrl, p.price, COUNT(ci) FROM CustomerInteraction ci JOIN ci.product p WHERE ci.eventType = 'AI_RECOMMENDED' GROUP BY p.id, p.name, p.imageUrl, p.price ORDER BY COUNT(ci) DESC")
    List<Object[]> findTopRecommendedProducts();

    @Query("SELECT p.id, p.name, p.imageUrl, p.price, COUNT(ci) FROM CustomerInteraction ci JOIN ci.product p WHERE ci.eventType = 'VIEW' GROUP BY p.id, p.name, p.imageUrl, p.price ORDER BY COUNT(ci) DESC")
    List<Object[]> findTopViewedProducts();

    @Query("SELECT AVG(ci.rating) FROM CustomerInteraction ci WHERE ci.rating IS NOT NULL")
    Double findAverageRating();
}
