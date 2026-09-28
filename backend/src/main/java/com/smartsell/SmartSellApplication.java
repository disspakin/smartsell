package com.smartsell;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

// @EnableScheduling เปิดให้ AuthThrottleService กวาดตัวนับที่หมดอายุทิ้งเป็นระยะ
@EnableScheduling
@SpringBootApplication
public class SmartSellApplication {
    public static void main(String[] args) {
        SpringApplication.run(SmartSellApplication.class, args);
    }
}
