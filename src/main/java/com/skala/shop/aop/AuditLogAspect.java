package com.skala.shop.aop;

import com.skala.shop.exception.BusinessException;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class AuditLogAspect {

    // 일반 애플리케이션 로그와 분리된 감사 전용 로거 (logback-spring.xml에서 별도 파일로 라우팅)
    private static final Logger auditLog = LoggerFactory.getLogger("AUDIT");

    private final MeterRegistry meterRegistry;

    public AuditLogAspect(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Pointcut("execution(* com.skala.shop.service.OrderService.order(..))")
    public void orderPointcut() {
    }

    @Pointcut("execution(* com.skala.shop.service.OrderService.cancel(..))")
    public void cancelPointcut() {
    }

    @Around("orderPointcut() || cancelPointcut()")
    public Object logOrderAudit(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();
        String customerId = args.length > 0 ? String.valueOf(args[0]) : "UNKNOWN";
        String requestDetail = args.length > 1 ? String.valueOf(args[1]) : "-";

        Timer.Sample sample = Timer.start(meterRegistry);
        String resultStatus = "SUCCESS"; // 기본값, 예외 시 아래에서 갱신

        try {
            Object result = joinPoint.proceed();

            auditLog.info("method={} customerId={} request={} result={} status=SUCCESS",
                    methodName, customerId, requestDetail, result);

            return result;

        } catch (BusinessException e) {
            // 비즈니스 규칙에 의한 정상적인 거부 (포인트 부족, 재고 부족 등) - 시스템 장애가 아님
            resultStatus = "REJECTED";

            auditLog.warn("method={} customerId={} request={} status=REJECTED errorCode={}",
                    methodName, customerId, requestDetail, e.getErrorCode());

            throw e;

        } catch (Exception e) {
            resultStatus = "FAILED";

            auditLog.error("method={} customerId={} request={} status=FAILED exception={} message={}",
                    methodName, customerId, requestDetail, e.getClass().getSimpleName(), e.getMessage());

            throw e;
        } finally {
            sample.stop(Timer.builder("order.service.duration")
                    .description("OrderService 메서드 실행 시간")
                    .tag("method", methodName)
                    .tag("result", resultStatus)
                    .register(meterRegistry));
        }
    }
}