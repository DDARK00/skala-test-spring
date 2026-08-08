package com.skala.shop.exception;

import lombok.Getter;
import java.time.LocalDateTime;

@Getter
public class ErrorResponse {
    private final String errorCode;
    private final String message;
    private final LocalDateTime timestamp;
    private final String path;

    public ErrorResponse(ErrorCode errorCode, String path) {
        this.errorCode = errorCode.name();
        this.message = errorCode.getMessage();
        this.timestamp = LocalDateTime.now();
        this.path = path;
    }
}