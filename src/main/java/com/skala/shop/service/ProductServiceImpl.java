package com.skala.shop.service;

import com.skala.shop.data.dto.ProductRequestDto;
import com.skala.shop.data.dto.ProductResponseDto;
import com.skala.shop.data.entity.*;
import com.skala.shop.exception.BusinessException;
import com.skala.shop.exception.ErrorCode;
import com.skala.shop.repository.*;
import lombok.RequiredArgsConstructor;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final SupplierRepository supplierRepository;
    private final CategoryRepository categoryRepository;
    private final StockHistoryRepository stockHistoryRepository;

    @Override
    @Transactional
    public ProductResponseDto createProduct(ProductRequestDto request) {
        Supplier supplier = resolveSupplier(request.supplierId());
        Category category = resolveCategory(request.categoryId());

        Product product = Product.builder()
                .name(request.name())
                .price(request.price())
                .costPrice(request.costPrice())
                .stock(request.stock())
                .supplier(supplier)
                .category(category)
                .build();

        Product saved = productRepository.save(product);

        // 최초 등록 재고 = 입고 이력으로 적재 (0이면 기록 생략)
        if (request.stock() != null && request.stock() > 0) {
            stockHistoryRepository.save(StockHistory.builder()
                    .product(saved)
                    .amount(request.stock())
                    .unitCost(request.costPrice())
                    .reason(StockReason.PURCHASE_IN)
                    .createdAt(LocalDateTime.now())
                    .build());
        }

        return toResponseDto(saved);
    }

    @Override
    @Cacheable(value = "productList",
               key = "#pageable.pageNumber + '_' + #pageable.pageSize + '_' + #pageable.sort")
    public Page<ProductResponseDto> getProducts(Pageable pageable) {
        return productRepository.findAll(pageable).map(this::toResponseDto);
    }

    @Override
    @Cacheable(value = "products", key = "#id")
    public ProductResponseDto getProduct(Long id) {
        return toResponseDto(findProductOrThrow(id));
    }

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "products", key = "#id"),
        @CacheEvict(value = "productList", allEntries = true)
    })
    public ProductResponseDto updateProduct(Long id, ProductRequestDto request) {
        Product product = findProductOrThrow(id);
        Supplier supplier = resolveSupplier(request.supplierId());
        Category category = resolveCategory(request.categoryId());

        int previousStock = product.getStock();
        int newStock = request.stock();
        int diff = newStock - previousStock;

        product.update(request.name(), request.price(), request.costPrice(),
                request.stock(), supplier, category);

        // 재고 변경분만큼 이력 적재: 증가=입고, 감소=조정 (0이면 생략)
        if (diff != 0) {
            stockHistoryRepository.save(StockHistory.builder()
                    .product(product)
                    .amount(diff)
                    .unitCost(diff > 0 ? request.costPrice() : null)
                    .reason(diff > 0 ? StockReason.PURCHASE_IN : StockReason.ADJUSTMENT)
                    .createdAt(LocalDateTime.now())
                    .build());
        }

        return toResponseDto(product);
    }

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "products", key = "#id"),
        @CacheEvict(value = "productList", allEntries = true)
    })
    public void deleteProduct(Long id) {
        productRepository.delete(findProductOrThrow(id));
    }

    private Product findProductOrThrow(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND));
    }

    private Supplier resolveSupplier(Long supplierId) {
        if (supplierId == null)
            return null;
        return supplierRepository.findById(supplierId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND));
    }

    private Category resolveCategory(Long categoryId) {
        if (categoryId == null)
            return null;
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND));
    }

    private ProductResponseDto toResponseDto(Product product) {
        return new ProductResponseDto(
                product.getId(), product.getName(), product.getPrice(), product.getCostPrice(),
                product.getStock(),
                product.getSupplier() != null ? product.getSupplier().getId() : null,
                product.getCategory() != null ? product.getCategory().getId() : null);
    }
}