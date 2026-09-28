package com.smartsell.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {

    /** ความยาวขั้นต่ำของกุญแจสำหรับ HMAC-SHA ตาม RFC 7518 */
    private static final int MIN_SECRET_BYTES = 32;

    /** ค่าตัวอย่างที่เคยอยู่ในไฟล์ตั้งค่า ห้ามใช้จริงเพราะเปิดเผยอยู่ใน git */
    private static final String PLACEHOLDER_PREFIX = "CHANGE_THIS";

    // ไม่ใส่ค่า default ไว้โดยตั้งใจ — ถ้าลืมตั้ง app.jwt.secret ต้องให้แอปสตาร์ทไม่ขึ้น
    // ดีกว่าแอบสตาร์ทขึ้นด้วยกุญแจที่ใครก็รู้ แล้วปลอม token เข้ามาได้
    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.expiration-ms:86400000}")
    private long jwtExpirationMs;

    @PostConstruct
    void validateSecret() {
        if (jwtSecret == null || jwtSecret.isBlank()) {
            throw new IllegalStateException(
                    "ยังไม่ได้ตั้งค่า app.jwt.secret — สร้างด้วย: openssl rand -base64 64");
        }
        if (jwtSecret.startsWith(PLACEHOLDER_PREFIX)) {
            throw new IllegalStateException(
                    "app.jwt.secret ยังเป็นค่าตัวอย่าง กรุณาสุ่มค่าใหม่ด้วย: openssl rand -base64 64");
        }
        int length = jwtSecret.getBytes(StandardCharsets.UTF_8).length;
        if (length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "app.jwt.secret สั้นเกินไป (" + length + " ไบต์) ต้องยาวอย่างน้อย " + MIN_SECRET_BYTES + " ไบต์");
        }
    }

    // เดิมโค้ดเติมศูนย์ต่อท้ายกุญแจที่สั้นกว่า 32 ไบต์เพื่อให้ผ่านข้อกำหนดของ HS256
    // ซึ่งทำให้กุญแจอ่อนๆ ผ่านไปได้เงียบๆ จึงเปลี่ยนมาปฏิเสธตั้งแต่ตอนสตาร์ทแทน (validateSecret)
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(String email, String role, String displayName) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);

        return Jwts.builder()
                .subject(email)
                .claim("role", role)
                .claim("displayName", displayName)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    public String getEmailFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return claims.getSubject();
    }

    public String getRoleFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return claims.get("role", String.class);
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
