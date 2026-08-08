package com.skala.shop.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "shop.cookie")
@Getter
@Setter
public class CookieProperties {
    private boolean secure;
    private String sameSite;
}
