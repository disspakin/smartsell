package com.smartsell.service;

import com.smartsell.entity.OtpPurpose;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@DisplayName("EmailService")
class EmailServiceTest {

    private final JavaMailSender mailSender = mock(JavaMailSender.class);
    private final RestTemplate restTemplate = new RestTemplate();
    private final MockRestServiceServer resend = MockRestServiceServer.bindTo(restTemplate).build();

    @Test
    @DisplayName("ตั้ง Resend key ไว้ ส่งผ่าน Resend ไม่ใช้ SMTP")
    void sendsViaResendWhenKeySet() {
        EmailService service = new EmailService(mailSender, restTemplate, "re_test", "noreply@shop.test");
        resend.expect(requestTo(EmailService.RESEND_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer re_test"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.from").value("noreply@shop.test"))
                .andExpect(jsonPath("$.to[0]").value("staff@shop.test"))
                .andExpect(jsonPath("$.subject").value("หัวข้อ"))
                .andExpect(jsonPath("$.text").value("เนื้อหา"))
                .andRespond(withSuccess("{\"id\":\"1\"}", MediaType.APPLICATION_JSON));

        service.send("staff@shop.test", "หัวข้อ", "เนื้อหา");

        resend.verify();
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("ไม่ได้ตั้ง Resend key ส่งผ่าน SMTP แบบเดิม")
    void fallsBackToSmtpWithoutKey() {
        EmailService service = new EmailService(mailSender, restTemplate, "", "me@gmail.com");

        service.send("staff@shop.test", "หัวข้อ", "เนื้อหา");

        verify(mailSender).send(any(SimpleMailMessage.class));
        resend.verify();
    }

    @Test
    @DisplayName("Resend ตอบ error ตอนส่ง OTP ไม่โยน exception ออกไป")
    void otpSwallowsResendError() {
        EmailService service = new EmailService(mailSender, restTemplate, "re_test", "noreply@shop.test");
        resend.expect(requestTo(EmailService.RESEND_URL)).andRespond(withServerError());

        service.sendOtpEmail("staff@shop.test", "123456", 10, OtpPurpose.RESET_PASSWORD);

        resend.verify();
    }
}
