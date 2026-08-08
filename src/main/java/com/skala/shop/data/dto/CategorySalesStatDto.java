package com.skala.shop.data.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "카테고리별 판매 비중")
public record CategorySalesStatDto(
        @Schema(description = "카테고리 ID (미분류면 null)") Long categoryId,
        @Schema(description = "카테고리명 (미분류면 '미분류')") String categoryName,
        @Schema(description = "순 매출액") Long netRevenue) {
}