package com.skala.shop.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("SKALA-SHOP API")
                        .description("온라인 쇼핑몰 백엔드 REST API - 상품/고객/주문 관리")
                        .version("v1.0.0"));
    }
}