package com.skala.shop.controller;

import com.skala.shop.data.dto.CancelRateStatDto;
import com.skala.shop.data.dto.CategorySalesStatDto;
import com.skala.shop.data.dto.DailySalesStatDto;
import com.skala.shop.data.dto.ProductSalesStatDto;
import com.skala.shop.data.dto.SupplierPerformanceStatDto;
import com.skala.shop.service.StatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Stats", description = "판매자 통계 API (MyBatis 집계)")
@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;

    @Operation(summary = "상품별 판매 통계")
    @GetMapping("/products")
    public ResponseEntity<List<ProductSalesStatDto>> getProductSalesStats() {
        return ResponseEntity.ok(statsService.getProductSalesStats());
    }

    @Operation(summary = "일별 매출 추이")
    @GetMapping("/daily")
    public ResponseEntity<List<DailySalesStatDto>> getDailySalesStats() {
        return ResponseEntity.ok(statsService.getDailySalesStats());
    }

    @Operation(summary = "카테고리별 판매 비중")
    @GetMapping("/categories")
    public ResponseEntity<List<CategorySalesStatDto>> getCategorySalesStats() {
        return ResponseEntity.ok(statsService.getCategorySalesStats());
    }

    @Operation(summary = "상품별 취소율 순위")
    @GetMapping("/cancel-rate")
    public ResponseEntity<List<CancelRateStatDto>> getCancelRateStats() {
        return ResponseEntity.ok(statsService.getCancelRateStats());
    }

    @Operation(summary = "공급업체별 매입 대비 판매 실적")
    @GetMapping("/suppliers")
    public ResponseEntity<List<SupplierPerformanceStatDto>> getSupplierPerformanceStats() {
        return ResponseEntity.ok(statsService.getSupplierPerformanceStats());
    }
}