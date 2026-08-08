package com.skala.shop.data.dto;

import io.swagger.v3.oas.annotations.media.Schema;

// GET /api/customers/{customerId} 전체 응답 (이미지 시나리오④ #5번 형태)
@Schema(description = "고객 상세 정보 응답 (주문상품 목록 포함)")
public record CustomerDetailResponseDto(
                @Schema(description = "고객 ID", example = "skala01") String customerId,
                @Schema(description = "보유 포인트", example = "970000") Long point,
                @Schema(description = "현재 주문 중인 상품 목록") java.util.List<CustomerProductDto> products) {
}