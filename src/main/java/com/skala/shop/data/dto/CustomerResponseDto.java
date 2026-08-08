package com.skala.shop.data.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "고객 기본 정보 응답 (목록 조회용)")
public record CustomerResponseDto(
        @Schema(description = "고객 ID", example = "skala01") String customerId,
        @Schema(description = "고객 이름", example = "홍길동") String customerName,
        @Schema(description = "보유 포인트", example = "10000") Long point) {
}