package com.skala.shop.data.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "상품별 판매 통계 (순매출 기준)")
public record ProductSalesStatDto(
        @Schema(description = "상품 ID") Long productId,
        @Schema(description = "상품명") String productName,
        @Schema(description = "순 판매 수량 (주문-취소)") Long netQuantity,
        @Schema(description = "순 매출액") Long netRevenue,
        @Schema(description = "총 마진 (원가 정보 없으면 null)") Long totalMargin) {
}