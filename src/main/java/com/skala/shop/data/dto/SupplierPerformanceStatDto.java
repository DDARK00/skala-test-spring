package com.skala.shop.data.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "공급업체별 매입 대비 판매 실적")
public record SupplierPerformanceStatDto(
        @Schema(description = "공급업체 ID") Long supplierId,
        @Schema(description = "공급업체명") String supplierName,
        @Schema(description = "총 매입액 (입고 * 단가)") Long totalPurchaseAmount,
        @Schema(description = "총 판매액 (순매출)") Long totalSalesAmount) {
}