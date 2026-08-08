package com.skala.shop.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(
            BusinessException e, HttpServletRequest request) {

        ErrorCode code = e.getErrorCode();
        HttpStatus status = switch (code) {
            case DATA_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case INSUFFICIENT_FUNDS, OUT_OF_STOCK -> HttpStatus.CONFLICT;
            case PARAMETER_EXCEPTION, DUPLICATE_CUSTOMER_ID -> HttpStatus.BAD_REQUEST;
            case UNAUTHORIZED, INVALID_CREDENTIALS -> HttpStatus.UNAUTHORIZED;
            default -> HttpStatus.INTERNAL_SERVER_ERROR; // INTERNAL_SERVER_ERROR 등 예상 외 값 대비
        };

        log.warn("BusinessException: {} - {}", code, request.getRequestURI());
        return ResponseEntity.status(status)
                .body(new ErrorResponse(code, request.getRequestURI()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException e, HttpServletRequest request) {

        log.warn("Validation failed: {} - {}", request.getRequestURI(), e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(ErrorCode.PARAMETER_EXCEPTION, request.getRequestURI()));
    }

    // 잘못된 JSON body (필드 타입 불일치, 문법 오류 등)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleNotReadableException(
            HttpMessageNotReadableException e, HttpServletRequest request) {

        log.warn("Malformed request body: {} - {}", request.getRequestURI(), e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(ErrorCode.PARAMETER_EXCEPTION, request.getRequestURI()));
    }

    // 그 외 모든 예상 못한 예외 - 마지막 방어선
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpectedException(
            Exception e, HttpServletRequest request) {

        log.error("Unexpected exception at {}", request.getRequestURI(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(ErrorCode.INTERNAL_SERVER_ERROR, request.getRequestURI()));
    }
}