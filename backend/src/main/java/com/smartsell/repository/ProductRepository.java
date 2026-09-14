package com.smartsell.repository;

import com.smartsell.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByCategory(String category);

    // simple starter query for the recommendation engine:
    // match products whose tags overlap with the customer's personal color / occasion
    @Query("""
        SELECT DISTINCT p FROM Product p JOIN p.tags t
        WHERE (t.tagType = 'SEASON' AND t.tagValue = :season)
           OR (t.tagType = 'OCCASION' AND t.tagValue = :occasion)
        """)
    List<Product> findMatchingSeasonOrOccasion(String season, String occasion);

    // used by the Lucky Color feature — matches products tagged with today's lucky color
    @Query("""
        SELECT DISTINCT p FROM Product p JOIN p.tags t
        WHERE t.tagType = 'LUCKY_COLOR' AND t.tagValue = :color
        """)
    List<Product> findByLuckyColor(String color);
}
