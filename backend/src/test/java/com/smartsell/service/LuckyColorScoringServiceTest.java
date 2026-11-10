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
    @DisplayName("สินค้าหลายสีแนะนำได้ทุกวัน แต่ในสีที่ตรงกับวงกลมสีของวันนั้น")
    void multiColorProductIsRecommendedInTheRightColor() {
        Product polo = product(1, "โปโล", "ขาว", "ดำ", "กรม", "เหลือง", "เทา");
        when(productRepository.findAllWithVariants()).thenReturn(List.of(polo));

        // จันทร์ (วงกลม: เขียว ดำ ขาว ม่วง): ขาวกับดำคะแนนเท่ากัน → เรียงตาม id ของสี
        assertThat(picks("จันทร์", null)).containsExactly("โปโล:ขาว", "โปโล:ดำ");
        // พฤหัส + การเงิน (วงกลม: เหลือง เขียว ทอง ดำ): เหลืองตรงทั้งคู่ = 3 มาก่อนดำที่ตรงแค่การเงิน = 2
        // กรมเป็นสีวันพฤหัสแต่ตกรอบวงกลม จึงไม่ถูกแนะนำ
        assertThat(picks("พฤหัส", "การเงิน")).containsExactly("โปโล:เหลือง", "โปโล:ดำ");
    }

    @Test
    @DisplayName("สินค้าที่มีหลายสีตรงขึ้นได้หลายการ์ด คนละสี")
    void multiColorProductGetsOneCardPerMatchingColor() {
        Product manyColors = product(1, "สีพื้น", "ส้ม", "ชมพู", "เทา", "เบจ-เขียว", "วอลนัท-เหลือง", "เขียวมะนาว");
        Product oneColor = product(2, "เขียวล้วน", "เขียว");
        when(productRepository.findAllWithVariants()).thenReturn(List.of(manyColors, oneColor));

        // จันทร์ + การเรียน (วงกลม: เขียว เหลือง ส้ม ฟ้า): เขียวตรงทั้งคู่ = 3 ก่อน ส้มตรงแค่การเรียน = 2
        // เสื้อสองสีใช้สีหลัก: เบจ-เขียว = เบจ, วอลนัท-เหลือง = น้ำตาล ไม่อยู่ในวงกลม จึงไม่ถูกแนะนำ
        assertThat(scoring.recommend("จันทร์", "การเรียน", null, 10).stream()
                .map(p -> p.name() + ":" + p.luckyColor()).toList())
                .containsExactly("สีพื้น:เขียวมะนาว", "เขียวล้วน:เขียว", "สีพื้น:ส้ม");
    }

    @Test
    @DisplayName("สีวันเกิดที่ตกรอบวงกลมสีไม่ถูกแนะนำ")
    void dayColorOutsideCirclesIsNotRecommended() {
        Product green = product(1, "เขียวล้วน", "เขียว");
        Product white = product(2, "ขาวล้วน", "ขาว");   // ขาวเป็นสีวันจันทร์ แต่จันทร์ + การเรียน วงกลมคือ เขียว เหลือง ส้ม ฟ้า
        when(productRepository.findAllWithVariants()).thenReturn(List.of(green, white));

        assertThat(picks("จันทร์", "การเรียน")).containsExactly("เขียวล้วน:เขียว");
        // ไม่เลือกด้านที่เสริม วงกลมเป็นสีวันจันทร์ ขาวจึงกลับมา
        assertThat(picks("จันทร์", null)).containsExactly("เขียวล้วน:เขียว", "ขาวล้วน:ขาว");
    }

    @Test
    @DisplayName("เสื้อสองสีนับเฉพาะสีหลัก (สีแรก) และการ์ดยังแสดงชื่อสีเต็ม")
    void twoToneShirtMatchesByPrimaryColor() {
        Product blackCream = product(1, "ดำ-ครีม", "ดำ-ครีม");   // สีหลัก ดำ
        Product grayOrange = product(2, "เทา-ส้ม", "เทา-ส้ม");   // สีหลัก เทา — ส้มเป็นแค่สีรอง ไม่นับ
        when(productRepository.findAllWithVariants()).thenReturn(List.of(blackCream, grayOrange));

        // จันทร์ + การเงิน (วงกลม: เขียว ดำ ม่วง ทอง)
        assertThat(picks("จันทร์", "การเงิน")).containsExactly("ดำ-ครีม:ดำ-ครีม");
        // จันทร์ + การเรียน (วงกลม: เขียว เหลือง ส้ม ฟ้า) — ไม่มีสีหลักไหนตรง
        assertThat(picks("จันทร์", "การเรียน")).isEmpty();
    }

    @Test
    @DisplayName("สีหลักคือส่วนก่อนขีด")
    void primaryColorIsTheFirstPart() {
        assertThat(LuckyColorScoringService.primaryColor("ดำ-น้ำเงิน")).isEqualTo("ดำ");
        assertThat(LuckyColorScoringService.primaryColor(" ม่วง - ครีม ")).isEqualTo("ม่วง");
        assertThat(LuckyColorScoringService.primaryColor("เขียวมะนาว")).isEqualTo("เขียวมะนาว");
    }

    @Test
    @DisplayName("สีที่ตรงแค่ด้านที่เสริมมาก่อนสีที่ตรงแค่วันเกิด")
    void goalOnlyColorBeatsDayOnlyColor() {
        Product green = product(1, "เขียวล้วน", "เขียว"); // จันทร์ + ความรัก: เขียวได้ช่องสำรองของสีวันเกิด → 1
        Product pink = product(2, "ชมพูล้วน", "ชมพู");   // แค่ความรัก → 2
        Product other = product(3, "เทาล้วน", "เทา");    // ไม่ตรงทั้งคู่ → ไม่ถูกแนะนำ
        when(productRepository.findAllWithVariants()).thenReturn(List.of(green, pink, other));

        assertThat(picks("จันทร์", "ความรัก")).containsExactly("ชมพูล้วน:ชมพู", "เขียวล้วน:เขียว");
    }

    @Test
    @DisplayName("แนะนำเฉพาะสีที่ตรง ไม่เกินจำนวนที่ขอ และมีไม่ถึงก็แสดงเท่าที่มี")
    void onlyMatchingProductsUpToLimit() {
        List<Product> greens = new java.util.ArrayList<>();
        for (long id = 1; id <= 12; id++) greens.add(product(id, "เขียว" + id, "เขียว"));
        greens.add(product(13, "เทาล้วน", "เทา"));
        when(productRepository.findAllWithVariants()).thenReturn(greens);

        List<LuckyColorDTO.LuckyProduct> picks = scoring.recommend("จันทร์", null, null, 10);
        assertThat(picks).hasSize(10).extracting(LuckyColorDTO.LuckyProduct::name).doesNotContain("เทาล้วน");

        when(productRepository.findAllWithVariants()).thenReturn(List.of(greens.get(0), greens.get(12)));
        assertThat(scoring.recommend("จันทร์", null, null, 10)).hasSize(1);
    }

    @Test
    @DisplayName("ใช้รูปและสีของการ์ดนั้น ไม่ใช่รูปของสีแรก")
    void usesImageOfRecommendedColor() {
        Product polo = product(1, "โปโล", "เทา", "เขียว");
        when(productRepository.findAllWithVariants()).thenReturn(List.of(polo));

        LuckyColorDTO.LuckyProduct pick = scoring.recommend("พุธ", null, null, 1).get(0); // พุธ: เขียว เหลืองอ่อน เหลืองแก่

        assertThat(pick.luckyColor()).isEqualTo("เขียว");
        assertThat(pick.imageUrl()).isEqualTo("/images/1-เขียว.png");
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
