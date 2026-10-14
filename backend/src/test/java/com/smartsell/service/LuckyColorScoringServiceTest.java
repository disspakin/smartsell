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
        return scoring.recommend(day, goal, null, 3).stream()
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
        Product twoTone = product(1, "ดำ-ครีม", "ดำ-ครีม");      // จันทร์ + การเงิน: ตรงบางส่วนทั้งคู่ → 3 × 0.6 = 1.8
        Product black = product(2, "ดำล้วน", "ดำ");               // จันทร์ + การเงิน: ตรงเป๊ะทั้งคู่ → 3
        Product white = product(3, "ขาวล้วน", "ขาว");             // แค่จันทร์ → 1
        when(productRepository.findAllWithVariants()).thenReturn(List.of(twoTone, black, white));

        assertThat(picks("จันทร์", "การเงิน")).containsExactly("ดำล้วน:ดำ", "ดำ-ครีม:ดำ-ครีม", "ขาวล้วน:ขาว");
    }

    @Test
    @DisplayName("สีที่ตรงแค่ด้านที่เสริมมาก่อนสีที่ตรงแค่วันเกิด")
    void goalOnlyColorBeatsDayOnlyColor() {
        Product white = product(1, "ขาวล้วน", "ขาว");   // แค่จันทร์ → 1
        Product pink = product(2, "ชมพูล้วน", "ชมพู");  // แค่ความรัก → 2
        Product other = product(3, "เทาล้วน", "เทา");   // ไม่ตรงทั้งคู่ → 0 ไม่ถูกแนะนำ
        when(productRepository.findAllWithVariants()).thenReturn(List.of(white, pink, other));

        assertThat(picks("จันทร์", "ความรัก")).containsExactly("ชมพูล้วน:ชมพู", "ขาวล้วน:ขาว");
        // ไม่เลือกด้านที่เสริม — มีแค่สีวันเกิดที่ได้คะแนน
        assertThat(picks("จันทร์", null)).containsExactly("ขาวล้วน:ขาว");
    }

    @Test
    @DisplayName("แนะนำเฉพาะสินค้าที่สีตรง และไม่เกินจำนวนที่ขอ")
    void onlyMatchingProductsUpToLimit() {
        List<Product> greens = new java.util.ArrayList<>();
        for (long id = 1; id <= 12; id++) greens.add(product(id, "เขียว" + id, "เขียว"));
        greens.add(product(13, "เทาล้วน", "เทา"));
        when(productRepository.findAllWithVariants()).thenReturn(greens);

        List<LuckyColorDTO.LuckyProduct> picks = scoring.recommend("จันทร์", null, null, 10);

        assertThat(picks).hasSize(10).extracting(LuckyColorDTO.LuckyProduct::name).doesNotContain("เทาล้วน");
    }

    @Test
    @DisplayName("สินค้าหนึ่งตัวขึ้นได้การ์ดเดียว")
    void oneCardPerProduct() {
        Product polo = product(1, "โปโล", "ขาว", "ดำ", "เขียว");
        Product other = product(2, "อื่น", "ม่วง");
        when(productRepository.findAllWithVariants()).thenReturn(List.of(polo, other));

        // โปโลมี 3 สีที่ตรงวันจันทร์ แต่ขึ้นได้การ์ดเดียว
        assertThat(scoring.recommend("จันทร์", null, null, 3)).extracting(LuckyColorDTO.LuckyProduct::name)
                .containsExactly("โปโล", "อื่น");
    }

    @Test
    @DisplayName("ใช้รูปและ hex ของสีที่แนะนำ ไม่ใช่รูปของสีแรก")
    void usesImageOfRecommendedColor() {
        Product polo = product(1, "โปโล", "เทา", "เหลือง");
        when(productRepository.findAllWithVariants()).thenReturn(List.of(polo));

        LuckyColorDTO.LuckyProduct pick = scoring.recommend("พุธ", null, null, 1).get(0); // พุธ: เขียว เหลืองอ่อน เหลืองแก่

        assertThat(pick.luckyColor()).isEqualTo("เหลือง");
        assertThat(pick.imageUrl()).isEqualTo("/images/1-เหลือง.png");
    }

    @Test
    @DisplayName("กรองตามเพศแบบเดียวกับผู้ช่วย AI และไม่แนะนำเสื้อเด็ก")
    void filtersByGenderAndExcludesKids() {
        Product men = product(1, "โปโลชาย", "เขียว");
        men.setTargetGroup("MALE");
        Product women = product(2, "โปโลหญิง", "เขียว");
        women.setTargetGroup("FEMALE");
        Product unisex = product(3, "เสื้อยืด", "เขียว");
        Product kids = product(4, "เสื้อเด็ก", "เขียว");
        kids.setTargetGroup("KIDS");
        when(productRepository.findAllWithVariants()).thenReturn(List.of(men, women, unisex, kids));

        assertThat(names("MALE")).containsExactly("โปโลชาย", "เสื้อยืด");
        assertThat(names("FEMALE")).containsExactly("โปโลหญิง", "เสื้อยืด");
        assertThat(names("ANY")).containsExactly("โปโลชาย", "โปโลหญิง", "เสื้อยืด");
        assertThat(names(null)).containsExactly("โปโลชาย", "โปโลหญิง", "เสื้อยืด");
    }

    private List<String> names(String gender) {
        return scoring.recommend("จันทร์", null, gender, 10).stream().map(LuckyColorDTO.LuckyProduct::name).toList();
    }
}
