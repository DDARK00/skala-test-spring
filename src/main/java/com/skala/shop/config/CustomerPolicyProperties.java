package com.skala.shop.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import lombok.Getter;
import lombok.Setter;

@Configuration
@ConfigurationProperties(prefix = "shop.customer")
@Getter
@Setter
public class CustomerPolicyProperties {
    private Long defaultPoint;
}