package com.skala.shop.data.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

@Schema(description = "상품 등록/수정 요청")
public record ProductRequestDto(
        @Schema(description = "상품명", example = "무선마우스") @NotBlank String name,
        @Schema(description = "판매가", example = "15000") @NotNull @Positive Long price,
        @Schema(description = "매입원가", example = "9000") Long costPrice,
        @Schema(description = "재고 수량", example = "100") @NotNull @PositiveOrZero Integer stock,
        @Schema(description = "공급업체 ID", example = "1") Long supplierId,
        @Schema(description = "카테고리 ID", example = "1") Long categoryId) {
}