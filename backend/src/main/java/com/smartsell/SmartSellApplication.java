package com.smartsell;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

// @EnableScheduling เปิดให้ AuthThrottleService กวาดตัวนับที่หมดอายุทิ้งเป็นระยะ
// @EnableAsync เปิดให้ EmailService ส่งเมลบน thread แยก ไม่บล็อกคำขอ HTTP
@EnableScheduling
@EnableAsync
@SpringBootApplication
public class SmartSellApplication {
    public static void main(String[] args) {
        SpringApplication.run(SmartSellApplication.class, args);
    }
}
