package com.smartsell.service;

import com.smartsell.dto.LuckyColorDTO;
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
@DisplayName("LuckyColorScoringService")
class LuckyColorScoringServiceTest {

    @Mock
    private ProductRepository productRepository;

    private LuckyColorScoringService scoring;
    private long nextVariantId = 1;

    @BeforeEach
    void setUp() {
        scoring = new LuckyColorScoringService(productRepository, new ColorRuleEngine());
    }

    /** สร้างสินค้า สีละ 2 ไซส์ ใส่รูปเฉพาะไซส์ L ให้เหมือนข้อมูลจริงที่บาง variant ไม่มีรูป */
    private Product product(long id, String name, String... colors) {
        Product p = new Product();
        p.setId(id);
        p.setName(name);
        p.setPrice(new BigDecimal("250"));
        for (String color : colors) {
            for (String size : List.of("M", "L")) {
                ProductVariant v = new ProductVariant();
                v.setId(nextVariantId++);
                v.setProduct(p);
                v.setColor(color);
                v.setSize(size);
                if (size.equals("L")) v.setImageUrl("/images/" + id + "-" + color + ".png");
                p.getVariants().add(v);
            }
        }
        return p;
    }

    private List<String> picks(String day, String goal) {
        return scoring.recommend(day, goal, 3).stream()
                .map(p -> p.name() + ":" + p.luckyColor())
                .toList();
    }

    @Test
    @DisplayName("สินค้าหลายสีแนะนำได้ทุกวัน แต่ในสีที่ตรงกับวันนั้น")
    void multiColorProductIsRecommendedInTheRightColor() {
        Product polo = product(1, "โปโล", "ขาว", "ดำ", "กรม", "เหลือง", "เทา");
        when(productRepository.findAllWithVariants()).thenReturn(List.of(polo));

        // จันทร์ (เขียว ดำ ขาว ม่วง): ขาวกับดำตรงเป๊ะเท่ากัน → ขาว เพราะ variant id น้อยกว่า
        assertThat(picks("จันทร์", null)).containsExactly("โปโล:ขาว");
        // พฤหัส (ส้ม เหลือง ฟ้า น้ำเงิน เขียว แดง): กรมกับเหลืองตรงเป๊ะเท่ากัน — เป้าหมายเป็นตัวตัดสิน
        assertThat(picks("พฤหัส", "การเงิน")).containsExactly("โปโล:เหลือง"); // การเงินมีเหลือง
        assertThat(picks("พฤหัส", "การงาน")).containsExactly("โปโล:กรม");     // การงานมีน้ำเงิน
    }

    @Test
    @DisplayName("สินค้าหลายสีไม่ถูกหักคะแนนเพราะมีสีที่ไม่ตรงปนอยู่")
    void otherColorsDoNotPenalize() {
        Product manyColors = product(1, "สีพื้น 6 สี", "ส้ม", "ชมพู", "เทา", "เบจ-เขียว", "วอลนัท-เหลือง", "เขียวมะนาว");
        Product oneColor = product(2, "เขียวล้วน", "เขียว");
        when(productRepository.findAllWithVariants()).thenReturn(List.of(manyColors, oneColor));

        // จันทร์: ทั้งคู่มีสีเขียวตรงเป๊ะ (เขียวมะนาว = ชื่อเรียกของเขียว) จึงคะแนนเท่ากัน เรียงตาม id
        assertThat(picks("จันทร์", null)).containsExactly("สีพื้น 6 สี:เขียวมะนาว", "เขียวล้วน:เขียว");
    }

    @Test
    @DisplayName("ตรงเป๊ะต้องชนะตรงบางส่วน และสีที่ตรงทั้งวันเกิดและเป้าหมายต้องมาก่อน")
    void exactAndGoalMatchesRankHigher() {
        Product twoTone = product(1, "ดำ-ครีม", "ดำ-ครีม");      // จันทร์: ตรงบางส่วน
        Product black = product(2, "ดำล้วน", "ดำ");               // จันทร์: ตรงเป๊ะ / การเงิน: ตรงเป๊ะ
        Product white = product(3, "ขาวล้วน", "ขาว");             // จันทร์: ตรงเป๊ะ / การเงิน: ไม่ตรง
        when(productRepository.findAllWithVariants()).thenReturn(List.of(twoTone, black, white));

        assertThat(picks("จันทร์", "การเงิน")).containsExactly("ดำล้วน:ดำ", "ขาวล้วน:ขาว", "ดำ-ครีม:ดำ-ครีม");
    }

    @Test
    @DisplayName("สินค้าหนึ่งตัวขึ้นได้การ์ดเดียว")
    void oneCardPerProduct() {
        Product polo = product(1, "โปโล", "ขาว", "ดำ", "เขียว");
        Product other = product(2, "อื่น", "เทา");
        when(productRepository.findAllWithVariants()).thenReturn(List.of(polo, other));

        assertThat(scoring.recommend("จันทร์", null, 3)).extracting(LuckyColorDTO.LuckyProduct::name)
                .containsExactly("โปโล", "อื่น");
    }

    @Test
    @DisplayName("ใช้รูปและ hex ของสีที่แนะนำ ไม่ใช่รูปของสีแรก")
    void usesImageOfRecommendedColor() {
        Product polo = product(1, "โปโล", "เทา", "เหลือง");
        when(productRepository.findAllWithVariants()).thenReturn(List.of(polo));

        LuckyColorDTO.LuckyProduct pick = scoring.recommend("พุธ", null, 1).get(0); // พุธ: เขียว เหลืองอ่อน เหลืองแก่

        assertThat(pick.luckyColor()).isEqualTo("เหลือง");
        assertThat(pick.imageUrl()).isEqualTo("/images/1-เหลือง.png");
    }
}
