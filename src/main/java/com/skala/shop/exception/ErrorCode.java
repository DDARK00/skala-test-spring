package com.skala.shop.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {
    INSUFFICIENT_FUNDS("포인트가 부족합니다."),
    DATA_NOT_FOUND("요청한 데이터를 찾을 수 없습니다."),
    PARAMETER_EXCEPTION("필수 입력값이 누락되었거나 형식이 올바르지 않습니다."),
    DUPLICATE_CUSTOMER_ID("이미 존재하는 고객 ID입니다."),
    INVALID_CREDENTIALS("아이디 또는 비밀번호가 일치하지 않습니다."),
    UNAUTHORIZED("로그인이 필요합니다."),
    OUT_OF_STOCK("재고가 부족합니다."),
    INTERNAL_SERVER_ERROR("서버 내부 오류가 발생했습니다.");

    private final String message;

    ErrorCode(String message) {
        this.message = message;
    }
}