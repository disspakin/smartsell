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

    @Query("SELECT ci.product.id, ci.product.name, COUNT(ci) FROM CustomerInteraction ci WHERE ci.product IS NOT NULL GROUP BY ci.product.id, ci.product.name ORDER BY COUNT(ci) DESC")
    List<Object[]> findTopInteractedProducts();

    @Query("SELECT p.id, p.name, p.imageUrl, p.price, COUNT(ci) FROM CustomerInteraction ci JOIN ci.product p WHERE ci.eventType = 'INTERESTED_CLICK' GROUP BY p.id, p.name, p.imageUrl, p.price ORDER BY COUNT(ci) DESC")
    List<Object[]> findTopInterestedProducts();

    @Query("SELECT p.id, p.name, p.imageUrl, p.price, COUNT(ci) FROM CustomerInteraction ci JOIN ci.product p WHERE ci.eventType = 'AI_RECOMMENDED' GROUP BY p.id, p.name, p.imageUrl, p.price ORDER BY COUNT(ci) DESC")
    List<Object[]> findTopRecommendedProducts();

    @Query("SELECT AVG(ci.rating) FROM CustomerInteraction ci WHERE ci.rating IS NOT NULL")
    Double findAverageRating();
}
