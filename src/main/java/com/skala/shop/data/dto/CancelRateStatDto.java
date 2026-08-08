package com.skala.shop.data.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "상품별 취소율 순위")
public record CancelRateStatDto(
        @Schema(description = "상품 ID") Long productId,
        @Schema(description = "상품명") String productName,
        @Schema(description = "총 주문 건수") Long totalOrderCount,
        @Schema(description = "총 취소 건수") Long totalCancelCount,
        @Schema(description = "취소율(%) - 소수 2자리") Double cancelRate) {
}