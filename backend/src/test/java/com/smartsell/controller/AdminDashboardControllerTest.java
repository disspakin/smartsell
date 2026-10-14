package com.smartsell.controller;

import com.smartsell.entity.CustomerInteraction;
import com.smartsell.entity.Product;
import com.smartsell.repository.CustomerInteractionRepository;
import com.smartsell.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@WithMockUser(roles = "STORE_MANAGER")
@DisplayName("Admin dashboard API — ตัวกรองช่วงเวลา")
class AdminDashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CustomerInteractionRepository interactionRepository;

    private Product product;

    @BeforeEach
    void setUp() {
        interactionRepository.deleteAll();
        Product p = new Product();
        p.setName("โปโลทดสอบ");
        p.setPrice(new BigDecimal("250.00"));
        product = productRepository.saveAndFlush(p);

        view(LocalDateTime.of(2026, 9, 30, 23, 59));   // ก.ย.
        view(LocalDateTime.of(2026, 10, 1, 0, 0));     // ต.ค. วันแรก
        view(LocalDateTime.of(2026, 10, 31, 23, 59));  // ต.ค. วันสุดท้าย นาทีสุดท้าย
        view(LocalDateTime.of(2025, 5, 1, 12, 0));     // ปีก่อน
    }

    private void view(LocalDateTime at) {
        CustomerInteraction ci = new CustomerInteraction(null, product, "VIEW", null, null, null, null, null);
        ci.setCreatedAt(at);
        interactionRepository.saveAndFlush(ci);
    }

    @Test
    @DisplayName("ไม่ส่งช่วงเวลา = นับทั้งหมด และบอกปีแรกที่มีข้อมูล")
    void allTime() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalViews").value(4))
                .andExpect(jsonPath("$.firstDataYear").value(2025));
    }

    @Test
    @DisplayName("รายเดือนนับครบทั้งวันแรกและวันสุดท้าย ไม่เกินขอบเดือน")
    void month() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard/summary").param("from", "2026-10-01").param("to", "2026-10-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalViews").value(2))
                .andExpect(jsonPath("$.topViewedProducts", hasSize(1)))
                .andExpect(jsonPath("$.topViewedProducts[0].count").value(2));
    }

    @Test
    @DisplayName("Product Analytics ยังแสดงสินค้าที่ไม่มีคนดูในช่วงนั้น (นับเป็น 0)")
    void productAnalyticsKeepsProductsWithoutEvents() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard/products/analytics").param("from", "2024-01-01").param("to", "2024-12-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.productName == 'โปโลทดสอบ')].viewCount").value(0));
    }

    @Test
    @DisplayName("วันที่ผิดรูปแบบ หรือวันเริ่มหลังวันสิ้นสุด ได้ 400")
    void rejectsBadRange() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard/summary").param("from", "2026-10-31").param("to", "2026-10-01"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/admin/dashboard/demand-analytics").param("from", "ตุลาคม"))
                .andExpect(status().isBadRequest());
    }
}
