package com.smartsell.controller;

import com.smartsell.entity.CustomerInteraction;
import com.smartsell.entity.Product;
import com.smartsell.repository.CustomerInteractionRepository;
import com.smartsell.repository.ProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Popular products API")
class PopularProductsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CustomerInteractionRepository interactionRepository;

    private Product product(String name) {
        Product p = new Product();
        p.setName(name);
        p.setPrice(new BigDecimal("150.00"));
        return productRepository.saveAndFlush(p);
    }

    private void events(Product p, String eventType, int count) {
        for (int i = 0; i < count; i++) {
            interactionRepository.saveAndFlush(new CustomerInteraction(null, p, eventType, null, null, null, null, null));
        }
    }

    @Test
    @DisplayName("เรียงตามจำนวน VIEW แล้วตาม INTERESTED_CLICK และตัดตาม limit")
    void ordersByViewsThenInterestedClicks() throws Exception {
        Product most = product("ดูเยอะสุด");
        Product tieLiked = product("ดูเท่ากัน แต่กดถูกใจ");
        Product tie = product("ดูเท่ากัน");
        events(most, "VIEW", 1000);
        events(tieLiked, "VIEW", 500);
        events(tieLiked, "INTERESTED_CLICK", 1);
        events(tie, "VIEW", 500);
        // AI_RECOMMENDED ไม่นับ — ต่อให้เยอะแค่ไหนก็ไม่ขึ้นมาก่อน
        events(tie, "AI_RECOMMENDED", 2000);

        mockMvc.perform(get("/api/products/popular?limit=3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].name").value("ดูเยอะสุด"))
                .andExpect(jsonPath("$[1].name").value("ดูเท่ากัน แต่กดถูกใจ"))
                .andExpect(jsonPath("$[2].name").value("ดูเท่ากัน"));
    }

    @Test
    @DisplayName("ค่าเริ่มต้น 6 ตัว — สินค้าที่ยังไม่มีคนดูก็ถูกเติมให้ครบ")
    void defaultsToSixAndFillsWithUnviewed() throws Exception {
        for (int i = 0; i < 7; i++) product("สินค้า " + i);

        mockMvc.perform(get("/api/products/popular"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(6)));
    }
}
