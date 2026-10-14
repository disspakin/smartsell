package com.smartsell.service;

import com.smartsell.dto.ProductDTO;
import com.smartsell.entity.Product;
import com.smartsell.entity.ProductVariant;
import com.smartsell.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductScoringService")
class ProductScoringServiceTest {

    @Mock
    private ProductRepository productRepository;

    private ProductScoringService scoring;

    @BeforeEach
    void setUp() {
        scoring = new ProductScoringService(productRepository, new ColorRuleEngine(), new OccasionRuleEngine());
        when(productRepository.findAllWithVariants()).thenReturn(List.of(
                product(1, "โปโลชาย", "MALE"),
                product(2, "โปโลหญิง", "FEMALE"),
                product(3, "เสื้อยืด", "UNISEX"),
                product(4, "เสื้อเด็ก", "KIDS")));
    }

    private Product product(long id, String name, String targetGroup) {
        Product p = new Product();
        p.setId(id);
        p.setName(name);
        p.setCategory("Polo");
        p.setPrice(new BigDecimal("250"));
        p.setTargetGroup(targetGroup);
        ProductVariant v = new ProductVariant();
        v.setId(id);
        v.setProduct(p);
        v.setColor("ขาว");
        v.setSize("M");
        p.getVariants().add(v);
        return p;
    }

    private List<String> names(String gender) {
        return scoring.recommendByPersonalColor("Winter", "ทั่วไป", null, null, "M", gender, 10).stream()
                .map(ProductDTO::name).toList();
    }

    @Test
    @DisplayName("กรองตามเพศ: ได้เสื้อเพศนั้นกับ unisex เท่านั้น")
    void filtersByGender() {
        assertThat(names("MALE")).containsExactlyInAnyOrder("โปโลชาย", "เสื้อยืด");
        assertThat(names("FEMALE")).containsExactlyInAnyOrder("โปโลหญิง", "เสื้อยืด");
    }

    @Test
    @DisplayName("ไม่ระบุเพศได้เสื้อผู้ใหญ่ทุกตัว และไม่มีเสื้อเด็กในทุกกรณี")
    void anyGenderExcludesKids() {
        assertThat(names("ANY")).containsExactlyInAnyOrder("โปโลชาย", "โปโลหญิง", "เสื้อยืด");
        assertThat(names(null)).doesNotContain("เสื้อเด็ก");
    }

    @Test
    @DisplayName("ส่ง targetGroup ไปให้หน้าเว็บด้วย")
    void exposesTargetGroup() {
        assertThat(scoring.recommendByPersonalColor("Winter", null, null, null, null, "FEMALE", 10))
                .extracting(ProductDTO::targetGroup).containsOnly("FEMALE", "UNISEX");
    }
}
