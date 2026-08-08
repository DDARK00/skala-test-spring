package com.skala.shop.data.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

// 주문 요청 (POST /api/customers/order)
@Schema(description = "상품 주문 요청")
public record OrderRequestDto(
                @Schema(description = "주문할 상품 ID", example = "1") @NotNull(message = "productId는 필수입니다.") Long productId,

                @Schema(description = "주문 수량", example = "2") @NotNull(message = "quantity는 필수입니다.") @Positive(message = "수량은 0보다 커야 합니다.") Integer quantity) {
}