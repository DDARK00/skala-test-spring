// ProductSalesStatDto와 같은 dto 패키지
package com.skala.shop.data.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "일별 매출 추이")
public record DailySalesStatDto(
        @Schema(description = "주문 일자 (yyyy-MM-dd)") String orderDate,
        @Schema(description = "순 판매 수량") Long netQuantity,
        @Schema(description = "순 매출액") Long netRevenue) {
}