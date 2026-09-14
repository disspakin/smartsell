package com.smartsell.config;

import com.smartsell.entity.*;
import com.smartsell.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private final AppUserRepository userRepository;
    private final ProductRepository productRepository;
    private final CustomerInteractionRepository interactionRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(AppUserRepository userRepository,
                           ProductRepository productRepository,
                           CustomerInteractionRepository interactionRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.interactionRepository = interactionRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        seedUsers();
        seedProducts();
        seedInteractions();
    }

    private void seedUsers() {
        if (userRepository.findByEmail("admin@utcc.ac.th").isEmpty()) {
            AppUser admin = new AppUser();
            admin.setEmail("admin@utcc.ac.th");
            admin.setPasswordHash(passwordEncoder.encode("password123"));
            admin.setRole("STORE_MANAGER");
            admin.setDisplayName("ผู้จัดการร้าน UTCC Shop");
            userRepository.save(admin);
        }
    }

    private void seedProducts() {
        if (productRepository.count() > 0) {
            return;
        }

        // Product 1: UTCC Polo Shirt - Gold
        Product poloGold = new Product();
        poloGold.setName("เสื้อโปโลมหาวิทยาลัย UTCC (สีเหลืองทอง)");
        poloGold.setCategory("Polo");
        poloGold.setPrice(new BigDecimal("350.00"));
        poloGold.setDescription("เสื้อโปโลผ้า Cotton เนื้อนุ่ม ระบายอากาศได้ดี ตราสัญลักษณ์ UTCC สีเหลืองทองสง่างาม");
        poloGold.setImageUrl("https://images.unsplash.com/photo-1581655353564-df123a1eb820?auto=format&fit=crop&w=600&q=80");
        createVariants(poloGold, "Yellow", "#EAB308", Arrays.asList("S", "M", "L", "XL", "XXL"));
        createTags(poloGold, "SEASON", "Warm Spring", "Autumn");
        createTags(poloGold, "OCCASION", "ใส่เรียน", "ทางการ", "เข้ากิจกรรม");
        createTags(poloGold, "LUCKY_COLOR", "การงาน/การเรียน", "การเงิน/โชคลาภ");
        productRepository.save(poloGold);

        // Product 2: UTCC Polo Shirt - Navy Blue
        Product poloNavy = new Product();
        poloNavy.setName("เสื้อโปโลมหาวิทยาลัย UTCC (สีน้ำเงินเข้ม)");
        poloNavy.setCategory("Polo");
        poloNavy.setPrice(new BigDecimal("350.00"));
        poloNavy.setDescription("เสื้อโปโลทรงสมาร์ท สีน้ำเงินเข้มคลาสสิก ปักโลโก้ UTCC ที่อกซ้าย เหมาะสำหรับใส่เรียนและงานทางการ");
        poloNavy.setImageUrl("https://images.unsplash.com/photo-1586363104862-3a5e2ab60d99?auto=format&fit=crop&w=600&q=80");
        createVariants(poloNavy, "Navy", "#1E3A8A", Arrays.asList("S", "M", "L", "XL"));
        createTags(poloNavy, "SEASON", "Cool Summer", "Winter");
        createTags(poloNavy, "OCCASION", "ใส่เรียน", "ทางการ");
        createTags(poloNavy, "LUCKY_COLOR", "การงาน/การเรียน", "สุขภาพ/แคล้วคลาด");
        productRepository.save(poloNavy);

        // Product 3: UTCC Polo Shirt - White
        Product poloWhite = new Product();
        poloWhite.setName("เสื้อโปโลมหาวิทยาลัย UTCC (สีขาวเรียบหรู)");
        poloWhite.setCategory("Polo");
        poloWhite.setPrice(new BigDecimal("350.00"));
        poloWhite.setDescription("เสื้อโปโลสีขาวสะอาด ตัดเย็บประณีต ผ้าซับเหงื่อได้ไว แมตช์กับกางเกงหรือกระโปรงได้ทุกสไตล์");
        poloWhite.setImageUrl("https://images.unsplash.com/photo-1625910513413-7fc214f479a3?auto=format&fit=crop&w=600&q=80");
        createVariants(poloWhite, "White", "#FFFFFF", Arrays.asList("S", "M", "L", "XL", "XXL"));
        createTags(poloWhite, "SEASON", "Summer", "Spring", "Winter");
        createTags(poloWhite, "OCCASION", "ใส่เรียน", "ทางการ", "ใส่ชิวๆ");
        createTags(poloWhite, "LUCKY_COLOR", "ความรัก/เมตตา", "การเงิน/โชคลาภ");
        productRepository.save(poloWhite);

        // Product 4: UTCC T-Shirt - Sky Blue
        Product tshirtSky = new Product();
        tshirtSky.setName("เสื้อยืดสกรีน UTCC Freshy (สีฟ้า)");
        tshirtSky.setCategory("T-Shirt");
        tshirtSky.setPrice(new BigDecimal("250.00"));
        tshirtSky.setDescription("เสื้อยืดคอกลมผ้า Supersoft สกรีนลาย UTCC Smart Campus สีฟ้าสดใส สวมใส่สบายสไตล์ชิลๆ");
        tshirtSky.setImageUrl("https://images.unsplash.com/photo-1521572267360-ee0c2909d518?auto=format&fit=crop&w=600&q=80");
        createVariants(tshirtSky, "Sky Blue", "#38BDF8", Arrays.asList("S", "M", "L", "XL"));
        createTags(tshirtSky, "SEASON", "Summer", "Spring");
        createTags(tshirtSky, "OCCASION", "ใส่ชิวๆ", "เข้ากิจกรรม");
        createTags(tshirtSky, "LUCKY_COLOR", "ความรัก/เมตตา", "สุขภาพ/แคล้วคลาด");
        productRepository.save(tshirtSky);

        // Product 5: UTCC T-Shirt - Black Premium
        Product tshirtBlack = new Product();
        tshirtBlack.setName("เสื้อยืดสกรีน UTCC Minimal (สีดำ)");
        tshirtBlack.setCategory("T-Shirt");
        tshirtBlack.setPrice(new BigDecimal("250.00"));
        tshirtBlack.setDescription("เสื้อยืดทรงโอเวอร์ไซส์สีดำ สกรีนอักษร UTCC สไตล์มินิมอล เท่ได้ทุกวัน");
        tshirtBlack.setImageUrl("https://images.unsplash.com/photo-1503342217505-b0a15ec3261c?auto=format&fit=crop&w=600&q=80");
        createVariants(tshirtBlack, "Black", "#000000", Arrays.asList("M", "L", "XL"));
        createTags(tshirtBlack, "SEASON", "Autumn", "Winter");
        createTags(tshirtBlack, "OCCASION", "ใส่ชิวๆ", "เข้ากิจกรรม");
        createTags(tshirtBlack, "LUCKY_COLOR", "การเงิน/โชคลาภ", "การงาน/การเรียน");
        productRepository.save(tshirtBlack);

        // Product 6: UTCC Campus Jacket Navy
        Product jacketNavy = new Product();
        jacketNavy.setName("เสื้อแจ็คเก็ตวอร์ม UTCC Sport (สีน้ำเงิน-ทอง)");
        jacketNavy.setCategory("Jacket");
        jacketNavy.setPrice(new BigDecimal("650.00"));
        jacketNavy.setDescription("เสื้อแจ็คเก็ตกีฬามหาวิทยาลัย UTCC ผ้าวอร์มพรีเมียม ซิปรูดลื่น ใส่กันลมหรือใส่ในห้องแอร์เย็นๆ");
        jacketNavy.setImageUrl("https://images.unsplash.com/photo-1551028719-00167b16eac5?auto=format&fit=crop&w=600&q=80");
        createVariants(jacketNavy, "Navy Gold", "#0F172A", Arrays.asList("M", "L", "XL"));
        createTags(jacketNavy, "SEASON", "Winter", "Autumn");
        createTags(jacketNavy, "OCCASION", "เข้ากิจกรรม", "ใส่ชิวๆ");
        createTags(jacketNavy, "LUCKY_COLOR", "การงาน/การเรียน", "สุขภาพ/แคล้วคลาด");
        productRepository.save(jacketNavy);
    }

    private void createVariants(Product p, String color, String colorHex, List<String> sizes) {
        for (String size : sizes) {
            ProductVariant v = new ProductVariant();
            v.setProduct(p);
            v.setColor(color);
            v.setColorHex(colorHex);
            v.setSize(size);
            p.getVariants().add(v);
        }
    }

    private void createTags(Product p, String tagType, String... values) {
        for (String val : values) {
            ProductTag t = new ProductTag();
            t.setProduct(p);
            t.setTagType(tagType);
            t.setTagValue(val);
            p.getTags().add(t);
        }
    }

    private void seedInteractions() {
        if (interactionRepository.count() > 0) return;

        List<Product> products = productRepository.findAll();
        if (products.isEmpty()) return;

        String[] personalColors = {"Spring", "Summer", "Autumn", "Winter"};
        String[] occasions = {"ใส่เรียน", "ใส่ชิวๆ", "เข้ากิจกรรม", "ทางการ"};
        String[] luckyColors = {"การงาน/การเรียน", "การเงิน/โชคลาภ", "ความรัก/เมตตา", "สุขภาพ/แคล้วคลาด"};
        String[] sizes = {"S", "M", "L", "XL"};
        String[] eventTypes = {"VIEW", "VIEW", "INTERESTED_CLICK", "AI_RECOMMENDED"};

        // Seed 40 realistic interactions
        for (int i = 0; i < 40; i++) {
            Product p = products.get(i % products.size());
            String pc = personalColors[i % personalColors.length];
            String occ = occasions[i % occasions.length];
            String lc = luckyColors[i % luckyColors.length];
            String sz = sizes[i % sizes.length];
            String evt = eventTypes[i % eventTypes.length];
            BigDecimal b = new BigDecimal(200 + (i * 25 % 500));

            CustomerInteraction ci = new CustomerInteraction(null, p, evt, pc, occ, lc, sz, b);
            if ("AI_RECOMMENDED".equals(evt) || "INTERESTED_CLICK".equals(evt)) {
                ci.setRating(4 + (i % 2)); // 4 or 5 stars
            }
            interactionRepository.save(ci);
        }
    }
}
