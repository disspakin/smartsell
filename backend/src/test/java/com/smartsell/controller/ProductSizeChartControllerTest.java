package com.smartsell.controller;

import com.smartsell.entity.Product;
import com.smartsell.entity.SizeChart;
import com.smartsell.entity.SizeChartRow;
import com.smartsell.repository.ProductRepository;
import com.smartsell.repository.SizeChartRepository;
import com.smartsell.repository.SizeChartRowRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Size chart API")
class ProductSizeChartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private SizeChartRepository sizeChartRepository;

    @Autowired
    private SizeChartRowRepository sizeChartRowRepository;

    private Product product(SizeChart chart) {
        Product p = new Product();
        p.setName("เสื้อทดสอบ");
        p.setPrice(new BigDecimal("200"));
        p.setSizeChart(chart);
        return productRepository.saveAndFlush(p);
    }

    private void row(SizeChart chart, String size, String chest, String length, int sortOrder) {
        SizeChartRow r = new SizeChartRow();
        r.setSizeChart(chart);
        r.setSize(size);
        r.setChestIn(new BigDecimal(chest));
        r.setLengthIn(new BigDecimal(length));
        r.setSortOrder(sortOrder);
        sizeChartRowRepository.saveAndFlush(r);
    }

    @Test
    @DisplayName("คืนตารางไซส์เรียงตาม sort_order ไม่ใช่ตามตัวอักษร")
    void returnsRowsInSortOrder() throws Exception {
        SizeChart chart = new SizeChart();
        chart.setName("T-Shirt คอกลม");
        chart = sizeChartRepository.saveAndFlush(chart);
        // บันทึกสลับลำดับ — ถ้าเรียงตามตัวอักษรจะได้ 2XL, M, S
        row(chart, "2XL", "46.0", "30.0", 3);
        row(chart, "S", "38.0", "26.0", 1);
        row(chart, "M", "40.0", "27.0", 2);
        Product p = product(chart);

        mockMvc.perform(get("/api/products/" + p.getId() + "/size-chart"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("T-Shirt คอกลม"))
                .andExpect(jsonPath("$.unit").value("inch"))
                .andExpect(jsonPath("$.isEstimated").value(false))
                .andExpect(jsonPath("$.rows[*].size", contains("S", "M", "2XL")))
                .andExpect(jsonPath("$.rows[0].chestIn").value(38.0))
                .andExpect(jsonPath("$.rows[0].lengthIn").value(26.0));
    }

    @Test
    @DisplayName("ตารางที่ is_estimated = true ส่ง isEstimated: true")
    void estimatedChartIsFlagged() throws Exception {
        SizeChart chart = new SizeChart();
        chart.setName("Crop");
        chart.setEstimated(true);
        chart = sizeChartRepository.saveAndFlush(chart);
        row(chart, "S", "36.0", "18.0", 1);
        Product p = product(chart);

        mockMvc.perform(get("/api/products/" + p.getId() + "/size-chart"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isEstimated").value(true));
    }

    @Test
    @DisplayName("สินค้าที่ไม่มีตารางไซส์ได้ 404")
    void productWithoutChartIs404() throws Exception {
        Product p = product(null);
        mockMvc.perform(get("/api/products/" + p.getId() + "/size-chart"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("สินค้าที่ไม่มีอยู่จริงได้ 404")
    void unknownProductIs404() throws Exception {
        mockMvc.perform(get("/api/products/999999/size-chart"))
                .andExpect(status().isNotFound());
    }
}
