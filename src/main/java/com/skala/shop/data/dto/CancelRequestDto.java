package com.skala.shop.data.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

// 주문 취소 요청 (POST /api/customers/cancel)
@Schema(description = "주문 취소 요청")
public record CancelRequestDto(
        @Schema(description = "취소할 상품 ID", example = "1") @NotNull(message = "productId는 필수입니다.") Long productId,

        @Schema(description = "취소 수량", example = "1") @NotNull(message = "quantity는 필수입니다.") @Positive(message = "수량은 0보다 커야 합니다.") Integer quantity) {
}