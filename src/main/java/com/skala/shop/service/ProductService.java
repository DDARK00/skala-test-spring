package com.skala.shop.service;

import com.skala.shop.data.dto.ProductRequestDto;
import com.skala.shop.data.dto.ProductResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProductService {
    ProductResponseDto createProduct(ProductRequestDto request);

    Page<ProductResponseDto> getProducts(Pageable pageable);

    ProductResponseDto getProduct(Long id);

    ProductResponseDto updateProduct(Long id, ProductRequestDto request);

    void deleteProduct(Long id);
}