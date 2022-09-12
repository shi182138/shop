package com.example.shop.product;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

/**
 * 1.整合mybatis-Plus
 *   1)、导入依赖
 *   2)、配置
 *    1、配置数据源
 *       1）、导入数据库的驱动
 *    2、配置mybatis-Plus
 *
 *
 * 2、逻辑删除
 *  1）、配置全局逻辑删除规则
 *
 *  2)、bean加上逻辑删除注解
 *
 *  3、
 *  4、统一异常处理
 *  5、自定义校验
 *    编写一个自定义校验注解
 *    编写一个自定义校验器
 *    关联自定义注解和自定义校验器
 *    **/

@EnableRedisHttpSession
@EnableCaching
@EnableFeignClients("com.example.shop.product.feign")
@EnableDiscoveryClient
@MapperScan("com.example.shop.product.dao")
@SpringBootApplication
public class ShopProductApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShopProductApplication.class, args);
    }

}
