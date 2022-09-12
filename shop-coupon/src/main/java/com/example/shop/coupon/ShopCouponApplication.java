package com.example.shop.coupon;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableDiscoveryClient
@SpringBootApplication
public class ShopCouponApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShopCouponApplication.class, args);
    }

}
