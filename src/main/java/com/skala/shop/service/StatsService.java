package com.skala.shop.service;

import com.skala.shop.data.dto.CancelRateStatDto;
import com.skala.shop.data.dto.CategorySalesStatDto;
import com.skala.shop.data.dto.DailySalesStatDto;
import com.skala.shop.data.dto.ProductSalesStatDto;
import com.skala.shop.data.dto.SupplierPerformanceStatDto;

import java.util.List;

public interface StatsService {
    List<ProductSalesStatDto> getProductSalesStats();

    List<DailySalesStatDto> getDailySalesStats();

    List<CategorySalesStatDto> getCategorySalesStats();

    List<CancelRateStatDto> getCancelRateStats();

    List<SupplierPerformanceStatDto> getSupplierPerformanceStats();
}