package com.smartsell.service;

import com.smartsell.entity.OtpPurpose;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * ส่งอีเมลผ่าน Resend ถ้าตั้ง app.mail.resend-api-key ไว้ ไม่งั้นส่งผ่าน SMTP ของ Gmail
 *
 * บน Railway แพ็กที่ไม่ใช่ Pro พอร์ต SMTP ถูกบล็อก จึงต้องใช้ Resend ซึ่งคุยผ่าน HTTPS
 * ตอนรันในเครื่องไม่ต้องตั้ง key ก็ส่งผ่าน Gmail ได้เหมือนเดิม
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    static final String RESEND_URL = "https://api.resend.com/emails";

    private final JavaMailSender mailSender;
    private final RestTemplate restTemplate;
    private final String resendApiKey;
    private final String from;

    @Autowired
    public EmailService(JavaMailSender mailSender,
                        @Value("${app.mail.resend-api-key:}") String resendApiKey,
                        @Value("${app.mail.from:${spring.mail.username:}}") String from) {
        this(mailSender, new RestTemplate(), resendApiKey, from);
    }

    EmailService(JavaMailSender mailSender, RestTemplate restTemplate, String resendApiKey, String from) {
        this.mailSender = mailSender;
        this.restTemplate = restTemplate;
        this.resendApiKey = resendApiKey == null ? "" : resendApiKey;
        this.from = from == null ? "" : from;
    }

    public void send(String to, String subject, String body) {
        if (!resendApiKey.isBlank()) {
            sendViaResend(to, subject, body);
            return;
        }
        SimpleMailMessage message = new SimpleMailMessage();
        if (!from.isBlank()) message.setFrom(from);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
    }

    // Resend บังคับให้มี from ที่อยู่ในโดเมนที่ยืนยันแล้ว ส่ง key ทาง header เท่านั้น ห้ามใส่ใน log
    private void sendViaResend(String to, String subject, String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(resendApiKey);
        Map<String, Object> payload = Map.of(
                "from", from,
                "to", List.of(to),
                "subject", subject,
                "text", body);
        restTemplate.postForEntity(RESEND_URL, new HttpEntity<>(payload, headers), String.class);
    }

    /**
     * ส่ง OTP แบบ @Async — การส่งเมลใช้เวลาเป็นวินาที ไม่ให้คำขอสมัครต้องรอ
     *
     * รันบน thread อื่น exception จึงไม่กลับไปถึงผู้เรียก ต้องจับและ log เองที่นี่
     * ถ้าส่งไม่สำเร็จ ผู้ใช้ยังกด "ส่งรหัสอีกครั้ง" ได้ ห้าม log ตัวรหัส
     */
    @Async
    public void sendOtpEmail(String to, String code, long validMinutes, OtpPurpose purpose) {
        boolean reset = purpose == OtpPurpose.RESET_PASSWORD;
        String subject = reset ? "SmartSell — รหัสสำหรับตั้งรหัสผ่านใหม่" : "SmartSell — รหัสยืนยันอีเมลของคุณ";
        String intro = reset ? "รหัสสำหรับตั้งรหัสผ่านใหม่ของคุณคือ " : "รหัสยืนยันอีเมลของคุณคือ ";
        String ignore = reset
                ? "ถ้าคุณไม่ได้ขอตั้งรหัสผ่านใหม่ ไม่ต้องทำอะไร รหัสผ่านเดิมของคุณยังใช้ได้ตามปกติ"
                : "ถ้าคุณไม่ได้สมัครบัญชี SmartSell ไม่ต้องทำอะไร เพิกเฉยอีเมลนี้ได้เลย";
        try {
            send(to, subject, intro + code + "\n\n"
                    + "รหัสนี้ใช้ได้ " + validMinutes + " นาที ห้ามบอกรหัสนี้กับผู้อื่น\n"
                    + ignore);
        } catch (RuntimeException e) {
            // MailException จาก SMTP หรือ RestClientException จาก Resend
            log.warn("[EmailService] ส่ง OTP ({}) ไปที่ {} ไม่สำเร็จ: {}", purpose, to, e.getMessage());
        }
    }
}
