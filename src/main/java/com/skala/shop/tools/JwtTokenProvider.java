package com.skala.shop.tools;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtTokenProvider {

    private final SecretKey key;
    private final long expirationMs;

    public JwtTokenProvider(
            @Value("${shop.jwt.secret}") String secret,
            @Value("${shop.jwt.expiration}") long expirationMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
        this.expirationMs = expirationMs;
    }

    /**
     * customerId를 subject로 담은 JWT를 발급한다.
     */
    public String generateToken(String customerId) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .setSubject(customerId)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 토큰에서 customerId(subject)를 추출한다.
     * 토큰이 유효하지 않으면 JwtException 계열 예외가 그대로 전파된다.
     */
    public String getCustomerId(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();

        return claims.getSubject();
    }

    /**
     * 토큰의 서명/만료 여부를 검증한다.
     * 만료(ExpiredJwtException)와 그 외 위변조/형식 오류를 구분해서 처리할 수 있도록 분리했다.
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (ExpiredJwtException e) {
            // 만료된 토큰 - 로깅 세분화 단계에서 별도 로그 레벨로 구분 가능
            return false;
        } catch (JwtException | IllegalArgumentException e) {
            // 서명 불일치, 형식 오류, null/빈 문자열 등
            return false;
        }
    }
}