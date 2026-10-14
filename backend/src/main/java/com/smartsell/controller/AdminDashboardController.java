package com.smartsell.controller;

import com.smartsell.entity.Product;
import com.smartsell.entity.ProductVariant;
import com.smartsell.repository.CustomerInteractionRepository;
import com.smartsell.repository.CustomerSessionRepository;
import com.smartsell.repository.ProductRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Stream;

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

    // ทุก endpoint รับ ?from=YYYY-MM-DD&to=YYYY-MM-DD ไม่บังคับ (ดู DashboardPeriod) — ไม่ส่ง = ข้อมูลทั้งหมด

    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getDashboardSummary(@RequestParam(required = false) String from,
                                                                   @RequestParam(required = false) String to) {
        DashboardPeriod period = DashboardPeriod.of(from, to);
        LocalDateTime start = period.start(), end = period.end();
        Map<String, Object> response = new HashMap<>();

        Double avgRating = interactionRepository.findAverageRating(start, end);
        response.put("averageRating", avgRating != null ? Math.round(avgRating * 10.0) / 10.0 : 0.0);

        response.put("totalSessions", sessionRepository.countCreatedBetween(start, end));
        response.put("totalViews", interactionRepository.countByEventTypeBetween("VIEW", start, end));
        response.put("totalInterestedClicks", interactionRepository.countByEventTypeBetween("INTERESTED_CLICK", start, end));

        putTopProducts(response, start, end);
        // ปีแรกที่มีข้อมูล (ค.ศ.) — หน้าเว็บใช้ทำ dropdown ปี
        response.put("firstDataYear", firstDataYear());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/demand-analytics")
    public ResponseEntity<Map<String, Object>> getDemandAnalytics(@RequestParam(required = false) String from,
                                                                  @RequestParam(required = false) String to) {
        DashboardPeriod period = DashboardPeriod.of(from, to);
        LocalDateTime start = period.start(), end = period.end();
        Map<String, Object> response = new HashMap<>();

        response.put("personalColorBreakdown", formatGroupResults(interactionRepository.countByPersonalColorGroup(start, end)));
        response.put("occasionBreakdown", formatGroupResults(interactionRepository.countByOccasionGroup(start, end)));
        response.put("luckyColorBreakdown", formatGroupResults(interactionRepository.countByLuckyColorGroup(start, end)));
        response.put("sizeBreakdown", formatGroupResults(interactionRepository.countBySizeGroup(start, end)));

        putTopProducts(response, start, end);

        return ResponseEntity.ok(response);
    }

    private void putTopProducts(Map<String, Object> response, LocalDateTime start, LocalDateTime end) {
        response.put("topInterestedProducts", formatProductList(interactionRepository.findTopProductsByEventType("INTERESTED_CLICK", start, end)));
        response.put("topRecommendedProducts", formatProductList(interactionRepository.findTopProductsByEventType("AI_RECOMMENDED", start, end)));
        response.put("topViewedProducts", formatProductList(interactionRepository.findTopProductsByEventType("VIEW", start, end)));
    }

    /** ปีแรกสุดที่มี session หรือ interaction — ยังไม่มีข้อมูลเลยใช้ปีปัจจุบัน */
    private int firstDataYear() {
        return Stream.of(sessionRepository.findFirstCreatedAt(), interactionRepository.findFirstCreatedAt())
                .filter(Objects::nonNull)
                .map(LocalDateTime::getYear)
                .min(Integer::compare)
                .orElse(LocalDate.now().getYear());
    }

    private List<Map<String, Object>> formatProductList(List<Object[]> rawList) {
        Map<Long, String> fallbackImages = variantImageFallbacks();
        List<Map<String, Object>> result = new ArrayList<>();
        int limit = Math.min(5, rawList.size());
        for (int i = 0; i < limit; i++) {
            Object[] row = rawList.get(i);
            Map<String, Object> item = new HashMap<>();
            item.put("productId", row[0]);
            item.put("productName", row[1]);
            item.put("imageUrl", resolveImage(row[0], row[2], fallbackImages));
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

    @GetMapping("/products/analytics")
    public ResponseEntity<List<Map<String, Object>>> getAllProductAnalytics(@RequestParam(required = false) String from,
                                                                          @RequestParam(required = false) String to) {
        DashboardPeriod period = DashboardPeriod.of(from, to);
        List<Object[]> rawList = interactionRepository.findAllProductAnalytics(period.start(), period.end());
        Map<Long, String> fallbackImages = variantImageFallbacks();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] row : rawList) {
            Map<String, Object> item = new HashMap<>();
            item.put("productId", row[0]);
            item.put("productName", row[1]);
            item.put("imageUrl", resolveImage(row[0], row[2], fallbackImages));
            item.put("price", row[3]);
            item.put("viewCount", row[4]);
            item.put("interestedCount", row[5]);
            item.put("recommendedCount", row[6]);
            result.add(item);
        }
        return ResponseEntity.ok(result);
    }

    // product หลายตัวไม่มีรูประดับ product มีแต่รูปของ variant — ใช้รูปของ variant แรกแทน (เหมือน ProductDTO)
    private Map<Long, String> variantImageFallbacks() {
        Map<Long, String> map = new HashMap<>();
        for (Product p : productRepository.findAllWithVariants()) {
            p.getVariants().stream()
                    .map(ProductVariant::getImageUrl)
                    .filter(url -> url != null && !url.isBlank())
                    .findFirst()
                    .ifPresent(url -> map.put(p.getId(), url));
        }
        return map;
    }

    private String resolveImage(Object productId, Object imageUrl, Map<Long, String> fallbackImages) {
        if (imageUrl instanceof String url && !url.isBlank()) return url;
        return fallbackImages.get((Long) productId);
    }
}
