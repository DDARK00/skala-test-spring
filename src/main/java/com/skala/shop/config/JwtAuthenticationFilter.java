package com.skala.shop.config;

import com.skala.shop.tools.JwtTokenProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    public static final String CUSTOMER_ID_ATTRIBUTE = "customerId";
    private static final String COOKIE_NAME = "bff-access";

    // MDC 키 상수 (다른 곳에서도 참조할 수 있게 public으로)
    public static final String MDC_REQUEST_ID = "requestId";
    public static final String MDC_CUSTOMER_ID = "customerId";

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String method = request.getMethod();

        if (uri.equals("/api/customers") && "POST".equalsIgnoreCase(method)) return true;
        if (uri.equals("/api/customers/login") && "POST".equalsIgnoreCase(method)) return true;
        if (uri.startsWith("/api/products") && "GET".equalsIgnoreCase(method)) return true;
        if (uri.startsWith("/api/stats")) return true;

        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {

        // 요청마다 고유 ID 부여 - 인증 실패로 401이 나가는 요청까지 전부 추적 가능하게 필터 최상단에서 세팅
        String requestId = UUID.randomUUID().toString().substring(0, 8);
        MDC.put(MDC_REQUEST_ID, requestId);

        try {
            String token = extractTokenFromCookie(request);

            if (token == null || !jwtTokenProvider.validateToken(token)) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "UNAUTHORIZED");
                return;
            }

            String customerId = jwtTokenProvider.getCustomerId(token);
            request.setAttribute(CUSTOMER_ID_ATTRIBUTE, customerId);
            MDC.put(MDC_CUSTOMER_ID, customerId);

            filterChain.doFilter(request, response);

        } finally {
            // 스레드 재사용 대비 - 반드시 정리
            MDC.clear();
        }
    }

    private String extractTokenFromCookie(HttpServletRequest request) {
        if (request.getCookies() == null) return null;
        for (Cookie cookie : request.getCookies()) {
            if (COOKIE_NAME.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}