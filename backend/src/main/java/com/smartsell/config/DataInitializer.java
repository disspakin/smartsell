package com.smartsell.config;

import com.smartsell.entity.*;
import com.smartsell.repository.*;
import com.smartsell.security.Roles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Locale;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private static final int MIN_PASSWORD_LENGTH = 8;

    /**
     * บัญชีเดโมที่เคยถูก seed ด้วยรหัสผ่านที่อยู่ใน git history
     * ใช้เพื่อตรวจหาและปิดบัญชีที่ยังค้างอยู่ในฐานข้อมูลเดิมเท่านั้น ไม่ได้ใช้สร้างบัญชี
     */
    private static final String LEGACY_DEMO_EMAIL = "admin@utcc.ac.th";
    private static final String LEGACY_DEMO_PASSWORD = "password123";

    private final AppUserRepository userRepository;
    private final ProductRepository productRepository;
    private final PasswordEncoder passwordEncoder;
    private final String seedAdminEmail;
    private final String seedAdminPassword;

    /**
     * อีเมล/รหัสผ่านบัญชีเริ่มต้นอ่านจาก app.seed.admin-email / app.seed.admin-password
     * (หรือ env APP_SEED_ADMIN_EMAIL / APP_SEED_ADMIN_PASSWORD) ไม่มีค่า default ที่เดาได้
     */
    public DataInitializer(AppUserRepository userRepository,
                           ProductRepository productRepository,
                           PasswordEncoder passwordEncoder,
                           @Value("${app.seed.admin-email:}") String seedAdminEmail,
                           @Value("${app.seed.admin-password:}") String seedAdminPassword) {
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.passwordEncoder = passwordEncoder;
        this.seedAdminEmail = seedAdminEmail == null ? "" : seedAdminEmail.trim().toLowerCase(Locale.ROOT);
        this.seedAdminPassword = seedAdminPassword == null ? "" : seedAdminPassword;
    }

    @Override
    public void run(String... args) {
        disableLegacyDemoAccount();
        seedUsers();
        seedProducts();
    }

    /**
     * ฐานข้อมูลที่เคยรันเวอร์ชันก่อนจะยังมีบัญชีเดโมที่ใช้รหัสผ่านสาธารณะค้างอยู่
     * เปลี่ยน hash เป็นค่าสุ่มที่ไม่มีใครรู้แทนการลบ เพราะอาจมีข้อมูลอื่นอ้างอิงแถวนี้อยู่
     * เจ้าของบัญชีตัวจริงยังกู้คืนได้ผ่าน /api/auth/forgot-password (ส่ง OTP ไปที่อีเมลของบัญชี)
     */
    private void disableLegacyDemoAccount() {
        userRepository.findByEmail(LEGACY_DEMO_EMAIL)
                .filter(user -> passwordEncoder.matches(LEGACY_DEMO_PASSWORD, user.getPasswordHash()))
                .ifPresent(user -> {
                    byte[] random = new byte[32];
                    new SecureRandom().nextBytes(random);
                    user.setPasswordHash(passwordEncoder.encode(Base64.getEncoder().encodeToString(random)));
                    userRepository.save(user);
                    log.warn("ปิดการใช้รหัสผ่านเดโมของบัญชี {} แล้ว — ใช้หน้ารีเซ็ตรหัสผ่านหากต้องการใช้บัญชีนี้ต่อ",
                            LEGACY_DEMO_EMAIL);
                });
    }

    private void seedUsers() {
        if (seedAdminEmail.isEmpty() || seedAdminPassword.isEmpty()) {
            log.info("ไม่ได้ตั้ง APP_SEED_ADMIN_EMAIL / APP_SEED_ADMIN_PASSWORD — ข้ามการสร้างบัญชีเริ่มต้น");
            return;
        }
        if (seedAdminPassword.length() < MIN_PASSWORD_LENGTH) {
            log.warn("APP_SEED_ADMIN_PASSWORD สั้นกว่า {} ตัวอักษร — ข้ามการสร้างบัญชีเริ่มต้น", MIN_PASSWORD_LENGTH);
            return;
        }
        if (userRepository.findByEmail(seedAdminEmail).isPresent()) {
            return;
        }

        AppUser admin = new AppUser();
        admin.setEmail(seedAdminEmail);
        admin.setPasswordHash(passwordEncoder.encode(seedAdminPassword));
        admin.setRole(Roles.STORE_MANAGER);
        admin.setDisplayName("ผู้จัดการร้าน UTCC Shop");
        // บัญชีที่ตั้งผ่าน config ถือว่ายืนยันแล้ว — ไม่มีขั้นตอนสมัครให้กรอก OTP
        admin.setEmailVerified(true);
        userRepository.save(admin);
        log.info("สร้างบัญชีเริ่มต้น {} แล้ว", seedAdminEmail);
    }

    private void seedProducts() {
        if (productRepository.count() > 0) {
            return;
        }

        // Product 1: UTCC Polo Shirt - Gold
        Product poloGold = new Product();
        poloGold.setName("เสื้อโปโลมหาวิทยาลัย UTCC (สีเหลืองทอง)");
        poloGold.setCategory("โปโล");
        poloGold.setPrice(new BigDecimal("250.00"));
        poloGold.setDescription("เสื้อโปโลผ้า Cotton เนื้อนุ่ม ระบายอากาศได้ดี ตราสัญลักษณ์ UTCC สีเหลืองทองสง่างาม");
        poloGold.setImageUrl("https://images.unsplash.com/photo-1581655353564-df123a1eb820?auto=format&fit=crop&w=600&q=80");
        createVariants(poloGold, "Yellow", "#EAB308", Arrays.asList("S", "M", "L", "XL", "XXL"));

        productRepository.save(poloGold);

        // Product 2: UTCC Polo Shirt - Navy Blue
        Product poloNavy = new Product();
        poloNavy.setName("เสื้อโปโลมหาวิทยาลัย UTCC (สีน้ำเงินเข้ม)");
        poloNavy.setCategory("โปโล");
        poloNavy.setPrice(new BigDecimal("250.00"));
        poloNavy.setDescription("เสื้อโปโลทรงสมาร์ท สีน้ำเงินเข้มคลาสสิก ปักโลโก้ UTCC ที่อกซ้าย เหมาะสำหรับใส่เรียนและงานทางการ");
        poloNavy.setImageUrl("https://images.unsplash.com/photo-1586363104862-3a5e2ab60d99?auto=format&fit=crop&w=600&q=80");
        createVariants(poloNavy, "Navy", "#1E3A8A", Arrays.asList("S", "M", "L", "XL"));

        productRepository.save(poloNavy);

        // Product 3: UTCC Polo Shirt - White
        Product poloWhite = new Product();
        poloWhite.setName("เสื้อโปโลมหาวิทยาลัย UTCC (สีขาวเรียบหรู)");
        poloWhite.setCategory("โปโล");
        poloWhite.setPrice(new BigDecimal("250.00"));
        poloWhite.setDescription("เสื้อโปโลสีขาวสะอาด ตัดเย็บประณีต ผ้าซับเหงื่อได้ไว แมตช์กับกางเกงหรือกระโปรงได้ทุกสไตล์");
        poloWhite.setImageUrl("https://images.unsplash.com/photo-1625910513413-7fc214f479a3?auto=format&fit=crop&w=600&q=80");
        createVariants(poloWhite, "White", "#FFFFFF", Arrays.asList("S", "M", "L", "XL", "XXL"));

        productRepository.save(poloWhite);

        // Product 4: UTCC T-Shirt - Sky Blue
        Product tshirtSky = new Product();
        tshirtSky.setName("เสื้อยืดสกรีน UTCC Freshy (สีฟ้า)");
        tshirtSky.setCategory("เสื้อยืด");
        tshirtSky.setPrice(new BigDecimal("180.00"));
        tshirtSky.setDescription("เสื้อยืดคอกลมผ้า Supersoft สกรีนลาย UTCC Smart Campus สีฟ้าสดใส สวมใส่สบายสไตล์ชิลๆ");
        tshirtSky.setImageUrl("https://images.unsplash.com/photo-1521572267360-ee0c2909d518?auto=format&fit=crop&w=600&q=80");
        createVariants(tshirtSky, "Sky Blue", "#38BDF8", Arrays.asList("S", "M", "L", "XL"));

        productRepository.save(tshirtSky);

        // Product 5: UTCC T-Shirt - Black Premium
        Product tshirtBlack = new Product();
        tshirtBlack.setName("เสื้อยืดสกรีน UTCC Minimal (สีดำ)");
        tshirtBlack.setCategory("เสื้อยืด");
        tshirtBlack.setPrice(new BigDecimal("200.00"));
        tshirtBlack.setDescription("เสื้อยืดทรงโอเวอร์ไซส์สีดำ สกรีนอักษร UTCC สไตล์มินิมอล เท่ได้ทุกวัน");
        tshirtBlack.setImageUrl("https://images.unsplash.com/photo-1503342217505-b0a15ec3261c?auto=format&fit=crop&w=600&q=80");
        createVariants(tshirtBlack, "Black", "#000000", Arrays.asList("M", "L", "XL"));

        productRepository.save(tshirtBlack);
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
}
