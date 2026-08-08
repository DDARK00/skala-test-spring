package com.skala.shop.data.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "상품 응답")
public record ProductResponseDto(
        @Schema(description = "상품 ID", example = "1") Long id,
        @Schema(description = "상품명", example = "무선마우스") String name,
        @Schema(description = "판매가", example = "15000") Long price,
        @Schema(description = "매입원가", example = "9000") Long costPrice,
        @Schema(description = "재고 수량", example = "100") Integer stock,
        @Schema(description = "공급업체 ID", example = "1") Long supplierId,
        @Schema(description = "카테고리 ID", example = "1") Long categoryId) {
}