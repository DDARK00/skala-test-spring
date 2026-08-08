package com.skala.shop.controller;

import com.skala.shop.data.dto.ProductRequestDto;
import com.skala.shop.data.dto.ProductResponseDto;
import com.skala.shop.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Product", description = "상품 관리 API")
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @Operation(summary = "상품 등록", description = "신규 상품을 등록합니다. (name, price, stock 필수)")
    @PostMapping
    public ResponseEntity<ProductResponseDto> create(@RequestBody @Valid ProductRequestDto request) {
        return ResponseEntity.ok(productService.createProduct(request));
    }

    @Operation(summary = "상품 전체 목록 조회", description = "페이지 단위로 상품 목록을 조회합니다.")
    @GetMapping
    public ResponseEntity<Page<ProductResponseDto>> getAll(Pageable pageable) {
        return ResponseEntity.ok(productService.getProducts(pageable));
    }

    @Operation(summary = "상품 상세 조회", description = "id에 해당하는 상품 상세 정보를 조회합니다.")
    @GetMapping("/{id}")
    public ResponseEntity<ProductResponseDto> get(
            @Parameter(description = "조회할 상품 ID", example = "1") @PathVariable Long id) {
        return ResponseEntity.ok(productService.getProduct(id));
    }

    @Operation(summary = "상품 정보 변경", description = "id에 해당하는 상품의 이름/가격/재고를 수정합니다.")
    @PutMapping("/{id}")
    public ResponseEntity<ProductResponseDto> update(
            @Parameter(description = "수정할 상품 ID", example = "1") @PathVariable Long id,
            @RequestBody @Valid ProductRequestDto request) {
        return ResponseEntity.ok(productService.updateProduct(id, request));
    }

    @Operation(summary = "상품 삭제", description = "id에 해당하는 상품을 삭제합니다.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @Parameter(description = "삭제할 상품 ID", example = "1") @PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }
}