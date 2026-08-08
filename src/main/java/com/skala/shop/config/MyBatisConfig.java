// config/MyBatisConfig.java - 신규 생성
package com.skala.shop.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("com.skala.shop.mapper")
public class MyBatisConfig {
}