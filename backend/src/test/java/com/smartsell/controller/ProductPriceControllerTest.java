package com.smartsell.controller;

import com.smartsell.entity.Product;
import com.smartsell.entity.ProductVariant;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("ราคาตามไซส์")
class ProductPriceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository productRepository;

    private ProductVariant variant(Product p, String size, String price) {
        ProductVariant v = new ProductVariant();
        v.setProduct(p);
        v.setColor("บานเย็น");
        v.setSize(size);
        v.setStockQty(10);
        v.setPrice(price == null ? null : new BigDecimal(price));
        p.getVariants().add(v);
        return v;
    }

    private Product product(String price) {
        Product p = new Product();
        p.setName("เสื้อทดสอบ");
        p.setPrice(new BigDecimal(price));
        return p;
    }

    @Test
    @DisplayName("variant ที่ price เป็น NULL ใช้ราคาปกติ ไซส์ที่มีราคาเองใช้ราคานั้น และ min/max เป็นช่วง")
    void effectivePriceAndRange() throws Exception {
        Product p = product("170.00");
        variant(p, "M", null);
        variant(p, "3XL", "180.00");
        p = productRepository.saveAndFlush(p);

        mockMvc.perform(get("/api/products/" + p.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.variants[?(@.size == 'M')].effectivePrice").value(170.0))
                .andExpect(jsonPath("$.variants[?(@.size == '3XL')].effectivePrice").value(180.0))
                .andExpect(jsonPath("$.minPrice").value(170.0))
                .andExpect(jsonPath("$.maxPrice").value(180.0));
    }

    @Test
    @DisplayName("ทุกไซส์ราคาเท่ากัน min = max = ราคาปกติ")
    void samePriceForAllSizes() throws Exception {
        Product p = product("160.00");
        variant(p, "S", null);
        variant(p, "L", null);
        p = productRepository.saveAndFlush(p);

        mockMvc.perform(get("/api/products/" + p.getId()))
                .andExpect(jsonPath("$.minPrice").value(160.0))
                .andExpect(jsonPath("$.maxPrice").value(160.0));
    }

    @Test
    @DisplayName("สินค้าที่ไม่มี variant ใช้ราคาปกติเป็น min/max")
    void noVariantsFallsBackToProductPrice() throws Exception {
        Product p = productRepository.saveAndFlush(product("200.00"));

        mockMvc.perform(get("/api/products/" + p.getId()))
                .andExpect(jsonPath("$.minPrice").value(200.0))
                .andExpect(jsonPath("$.maxPrice").value(200.0));
    }
}
