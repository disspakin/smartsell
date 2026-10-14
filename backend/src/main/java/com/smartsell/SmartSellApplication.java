package com.smartsell;

import com.smartsell.service.StoreInfoService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

// @EnableScheduling เปิดให้ AuthThrottleService กวาดตัวนับที่หมดอายุทิ้งเป็นระยะ
// @EnableAsync เปิดให้ EmailService ส่งเมลบน thread แยก ไม่บล็อกคำขอ HTTP
@EnableScheduling
@EnableAsync
@SpringBootApplication
public class SmartSellApplication {

    // ร้านอยู่ไทยที่เดียว: ให้แอปใช้เวลาไทยเสมอ ไม่ว่าเครื่องที่รันจะตั้งเขตเวลาอะไร
    // created_at ทุกตารางเก็บจาก LocalDateTime.now() — ถ้าไปรันบนเซิร์ฟเวอร์ UTC จะเพี้ยน 7 ชม.
    // และ Dashboard จะนับข้อมูลช่วงขอบวัน/ขอบเดือนผิดวัน
    // อยู่ใน static block (ไม่ใช่ใน main) เพื่อให้เทสต์ที่โหลดคลาสนี้ผ่าน @SpringBootTest ได้เวลาไทยเหมือนกัน
    static {
        TimeZone.setDefault(TimeZone.getTimeZone(StoreInfoService.STORE_ZONE));
    }

    public static void main(String[] args) {
        SpringApplication.run(SmartSellApplication.class, args);
    }
}
