package com.smartsell.service;

import com.smartsell.entity.OtpPurpose;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final String from;

    public EmailService(JavaMailSender mailSender,
                        @Value("${spring.mail.username:}") String from) {
        this.mailSender = mailSender;
        this.from = from;
    }

    public void send(String to, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        if (!from.isBlank()) message.setFrom(from);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
    }

    /**
     * ส่ง OTP แบบ @Async — คุยกับ SMTP ของ Gmail ใช้เวลาเป็นวินาที ไม่ให้คำขอสมัครต้องรอ
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
        } catch (MailException e) {
            log.warn("[EmailService] ส่ง OTP ({}) ไปที่ {} ไม่สำเร็จ: {}", purpose, to, e.getMessage());
        }
    }
}
