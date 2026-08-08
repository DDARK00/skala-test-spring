package com.skala.shop.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class RootController {

    @GetMapping("/")
    public Map<String, String> root() {
        return Map.of(
            "service", "SKALA-SHOP API",
            "status", "OK",
            "swagger", "/swagger-ui.html",
            "docs", "https://github.com/ddark00/skala-test-spring"
        );
    }
}
