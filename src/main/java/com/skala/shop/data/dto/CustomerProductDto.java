package com.skala.shop.data.dto;

import io.swagger.v3.oas.annotations.media.Schema;

// GET /api/customers/{customerId}/products 응답 항목
@Schema(description = "고객이 주문한 상품 단건 정보")
public record CustomerProductDto(
        @Schema(description = "상품 ID", example = "1") Long productId,
        @Schema(description = "상품명", example = "무선마우스") String productName,
        @Schema(description = "주문 수량", example = "2") Integer quantity,
        @Schema(description = "최근 주문 시각", example = "2026-08-07T14:20:00") java.time.LocalDateTime orderedAt) {
}