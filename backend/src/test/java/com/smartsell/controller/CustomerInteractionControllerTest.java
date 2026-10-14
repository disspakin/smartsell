package com.smartsell.controller;

import com.smartsell.entity.Product;
import com.smartsell.repository.CustomerInteractionRepository;
import com.smartsell.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Interaction API")
class CustomerInteractionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CustomerInteractionRepository interactionRepository;

    private Long productId;

    @BeforeEach
    void createProduct() {
        Product p = new Product();
        p.setName("เสื้อทดสอบ");
        p.setPrice(new BigDecimal("200"));
        productId = productRepository.saveAndFlush(p).getId();
    }

    private void postInteraction(String body, int expectedStatus) throws Exception {
        mockMvc.perform(post("/api/interactions").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().is(expectedStatus));
    }

    @Test
    @DisplayName("ลูกค้าที่ไม่มี session (ยังไม่เคยคุยกับผู้ช่วย AI) บันทึก VIEW ได้")
    void recordsViewWithoutSession() throws Exception {
        long before = countViews();

        postInteraction("{\"sessionId\":null,\"productId\":" + productId + ",\"eventType\":\"VIEW\"}", 200);
        postInteraction("{\"productId\":" + productId + ",\"eventType\":\"INTERESTED_CLICK\"}", 200);

        assertThat(countViews()).isEqualTo(before + 1);
    }

    private long countViews() {
        return interactionRepository.countByEventTypeBetween("VIEW", DashboardPeriod.EARLIEST, DashboardPeriod.LATEST);
    }

    @Test
    @DisplayName("ไม่ส่ง productId หรือ eventType ยังได้ 400")
    void stillRequiresProductAndEventType() throws Exception {
        postInteraction("{\"eventType\":\"VIEW\"}", 400);
        postInteraction("{\"productId\":" + productId + "}", 400);
    }

    @Test
    @DisplayName("session ที่ไม่มีอยู่จริงก็ยังบันทึกได้ แบบไม่ผูก session")
    void unknownSessionIsTreatedAsAnonymous() throws Exception {
        postInteraction("{\"sessionId\":999999,\"productId\":" + productId + ",\"eventType\":\"VIEW\"}", 200);
    }
}
