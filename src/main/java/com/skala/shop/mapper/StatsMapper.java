package com.skala.shop.mapper;

import com.skala.shop.data.dto.CancelRateStatDto;
import com.skala.shop.data.dto.CategorySalesStatDto;
import com.skala.shop.data.dto.DailySalesStatDto;
import com.skala.shop.data.dto.ProductSalesStatDto;
import com.skala.shop.data.dto.SupplierPerformanceStatDto;

import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface StatsMapper {
    List<ProductSalesStatDto> selectProductSalesStats();

    List<DailySalesStatDto> selectDailySalesStats();

    List<CategorySalesStatDto> selectCategorySalesStats();

    List<CancelRateStatDto> selectCancelRateStats();

    List<SupplierPerformanceStatDto> selectSupplierPerformanceStats();
}