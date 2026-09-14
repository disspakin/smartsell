package com.smartsell.controller;

import com.smartsell.repository.CustomerInteractionRepository;
import com.smartsell.repository.CustomerSessionRepository;
import com.smartsell.repository.ProductRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

@RestController
@RequestMapping("/api/admin/dashboard")
public class AdminDashboardController {

    private final CustomerInteractionRepository interactionRepository;
    private final CustomerSessionRepository sessionRepository;
    private final ProductRepository productRepository;

    public AdminDashboardController(CustomerInteractionRepository interactionRepository,
                                    CustomerSessionRepository sessionRepository,
                                    ProductRepository productRepository) {
        this.interactionRepository = interactionRepository;
        this.sessionRepository = sessionRepository;
        this.productRepository = productRepository;
    }

    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getDashboardSummary() {
        Map<String, Object> response = new HashMap<>();

        long totalInteractions = interactionRepository.count();
        long totalSessions = sessionRepository.count();
        long totalViews = interactionRepository.countByEventType("VIEW");
        long totalInterested = interactionRepository.countByEventType("INTERESTED_CLICK");
        long totalAiRecommends = interactionRepository.countByEventType("AI_RECOMMENDED");

        Double avgRating = interactionRepository.findAverageRating();
        response.put("averageRating", avgRating != null ? Math.round(avgRating * 10.0) / 10.0 : 4.8);

        response.put("topInterestedProducts", formatProductList(interactionRepository.findTopInterestedProducts()));
        response.put("topRecommendedProducts", formatProductList(interactionRepository.findTopRecommendedProducts()));

        return ResponseEntity.ok(response);
    }

    @GetMapping("/demand-analytics")
    public ResponseEntity<Map<String, Object>> getDemandAnalytics() {
        Map<String, Object> response = new HashMap<>();

        response.put("personalColorBreakdown", formatGroupResults(interactionRepository.countByPersonalColorGroup()));
        response.put("occasionBreakdown", formatGroupResults(interactionRepository.countByOccasionGroup()));
        response.put("luckyColorBreakdown", formatGroupResults(interactionRepository.countByLuckyColorGroup()));
        response.put("sizeBreakdown", formatGroupResults(interactionRepository.countBySizeGroup()));

        List<Object[]> topProductsRaw = interactionRepository.findTopInteractedProducts();
        List<Map<String, Object>> topProducts = new ArrayList<>();
        for (Object[] row : topProductsRaw) {
            Map<String, Object> p = new HashMap<>();
            p.put("productId", row[0]);
            p.put("productName", row[1]);
            p.put("interactionCount", row[2]);
            topProducts.add(p);
        }
        response.put("topProducts", topProducts);
        response.put("topInterestedProducts", formatProductList(interactionRepository.findTopInterestedProducts()));
        response.put("topRecommendedProducts", formatProductList(interactionRepository.findTopRecommendedProducts()));

        return ResponseEntity.ok(response);
    }

    private List<Map<String, Object>> formatProductList(List<Object[]> rawList) {
        List<Map<String, Object>> result = new ArrayList<>();
        int limit = Math.min(5, rawList.size());
        for (int i = 0; i < limit; i++) {
            Object[] row = rawList.get(i);
            Map<String, Object> item = new HashMap<>();
            item.put("productId", row[0]);
            item.put("productName", row[1]);
            item.put("imageUrl", row[2]);
            item.put("price", row[3]);
            item.put("count", row[4]);
            result.add(item);
        }
        return result;
    }

    private Map<String, Long> formatGroupResults(List<Object[]> queryResults) {
        Map<String, Long> map = new LinkedHashMap<>();
        for (Object[] row : queryResults) {
            if (row[0] != null) {
                map.put(row[0].toString(), (Long) row[1]);
            }
        }
        return map;
    }
}
