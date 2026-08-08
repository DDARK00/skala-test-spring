package com.skala.shop.service;

import com.skala.shop.data.dto.CancelRateStatDto;
import com.skala.shop.data.dto.CategorySalesStatDto;
import com.skala.shop.data.dto.DailySalesStatDto;
import com.skala.shop.data.dto.ProductSalesStatDto;
import com.skala.shop.data.dto.SupplierPerformanceStatDto;
import com.skala.shop.mapper.StatsMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StatsServiceImpl implements StatsService {

    private final StatsMapper statsMapper;

    @Override
    public List<ProductSalesStatDto> getProductSalesStats() {
        return statsMapper.selectProductSalesStats();
    }

    @Override
    public List<DailySalesStatDto> getDailySalesStats() {
        return statsMapper.selectDailySalesStats();
    }

    @Override
    public List<CategorySalesStatDto> getCategorySalesStats() {
        return statsMapper.selectCategorySalesStats();
    }

    @Override
    public List<CancelRateStatDto> getCancelRateStats() {
        return statsMapper.selectCancelRateStats();
    }

    @Override
    public List<SupplierPerformanceStatDto> getSupplierPerformanceStats() {
        return statsMapper.selectSupplierPerformanceStats();
    }
}