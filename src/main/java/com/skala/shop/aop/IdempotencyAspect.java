package com.skala.shop.aop;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skala.shop.data.entity.IdempotencyKey;
import com.skala.shop.exception.BusinessException;
import com.skala.shop.exception.ErrorCode;
import com.skala.shop.repository.IdempotencyKeyRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

@Aspect
@Component
@RequiredArgsConstructor
public class IdempotencyAspect {

    private static final String HEADER_NAME = "Idempotency-Key";

    private final IdempotencyKeyRepository idempotencyKeyRepository;
    private final ObjectMapper objectMapper;

    @Pointcut("execution(* com.skala.shop.controller.OrderController.order(..))")
    public void orderPointcut() {}

    @Pointcut("execution(* com.skala.shop.controller.OrderController.cancel(..))")
    public void cancelPointcut() {}

    @Around("orderPointcut() || cancelPointcut()")
    @Transactional
    public Object handleIdempotency(ProceedingJoinPoint joinPoint) throws Throwable {
        HttpServletRequest request = currentRequest();
        String idempotencyKey = request.getHeader(HEADER_NAME);
        String endpoint = request.getRequestURI();

        // 헤더가 없으면 멱등성 체크 없이 그냥 실행 (하위호환 - 강제하지 않음)
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return joinPoint.proceed();
        }

        var existing = idempotencyKeyRepository.findByKeyValueAndEndpoint(idempotencyKey, endpoint);
        if (existing.isPresent()) {
            // 이미 처리된 요청 - 실제 로직을 다시 실행하지 않고 저장된 응답을 그대로 반환
            IdempotencyKey saved = existing.get();
            Object body = objectMapper.readValue(saved.getResponseBody(), Object.class);
            return ResponseEntity.status(saved.getResponseStatus()).body(body);
        }

        Object result = joinPoint.proceed();

        if (result instanceof ResponseEntity<?> responseEntity) {
            idempotencyKeyRepository.save(IdempotencyKey.builder()
                    .keyValue(idempotencyKey)
                    .endpoint(endpoint)
                    .responseBody(objectMapper.writeValueAsString(responseEntity.getBody()))
                    .responseStatus(responseEntity.getStatusCode().value())
                    .createdAt(LocalDateTime.now())
                    .build());
        }

        return result;
    }

    private HttpServletRequest currentRequest() {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
        return attrs.getRequest();
    }
}