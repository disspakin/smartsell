package com.smartsell.controller;

import com.smartsell.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.regex.Pattern;

@RestController
public class MailTestController {

    private static final Logger log = LoggerFactory.getLogger(MailTestController.class);
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final EmailService emailService;

    public MailTestController(EmailService emailService) {
        this.emailService = emailService;
    }

    // GET /test-mail?to=someone@example.com — ส่งเมลทดสอบว่าตั้งค่า SMTP ถูกต้อง
    // หัวเรื่องและเนื้อหาตายตัว ผู้เรียกกำหนดได้แค่ผู้รับ
    @GetMapping("/test-mail")
    public ResponseEntity<Map<String, String>> sendTestMail(@RequestParam String to) {
        if (!EMAIL.matcher(to).matches()) {
            return ResponseEntity.badRequest().body(Map.of("error", "อีเมลผู้รับไม่ถูกต้อง"));
        }
        try {
            emailService.send(to, "SmartSell — ทดสอบการส่งอีเมล",
                    "อีเมลนี้ส่งจากระบบ SmartSell เพื่อทดสอบการตั้งค่า SMTP\nถ้าได้รับเมลนี้ แปลว่าตั้งค่าถูกต้องแล้ว");
            return ResponseEntity.ok(Map.of("message", "ส่งเมลทดสอบไปที่ " + to + " แล้ว"));
        } catch (MailException e) {
            log.warn("[MailTest] ส่งเมลไม่สำเร็จ: {}", e.getMessage());
            return ResponseEntity.status(502).body(Map.of("error", "ส่งเมลไม่สำเร็จ ตรวจสอบการตั้งค่า SMTP"));
        }
    }
}
