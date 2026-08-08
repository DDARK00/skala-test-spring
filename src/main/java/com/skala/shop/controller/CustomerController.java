package com.skala.shop.controller;

import com.skala.shop.config.CookieProperties;
import com.skala.shop.data.dto.*;
import com.skala.shop.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Customer", description = "고객 관리 및 인증 API")
@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;
    private final CookieProperties cookieProperties;

    @Operation(summary = "회원가입", description = "신규 고객을 등록합니다. point를 지정하지 않으면 기본 정책값(설정된 기본 포인트)이 지급되고, "
            + "이벤트 등으로 특정 포인트를 지급하려면 point 값을 직접 지정합니다.")
    @PostMapping
    public ResponseEntity<CustomerResponseDto> create(@RequestBody @Valid CustomerCreateRequestDto request) {
        return ResponseEntity.ok(customerService.createCustomer(request));
    }

    @Operation(summary = "로그인", description = "customerId/customerPassword로 인증 후 JWT를 발급해 Cookie(bff-access)로 내려줍니다. "
            + "이후 인증이 필요한 API는 이 Cookie로 고객을 식별합니다.")
    @PostMapping("/login")
    public ResponseEntity<Void> login(@RequestBody @Valid CustomerLoginRequestDto request,
            HttpServletResponse response) {
        String token = customerService.login(request);

        ResponseCookie cookie = ResponseCookie.from("bff-access", token)
                .httpOnly(true)
                .secure(cookieProperties.isSecure())      // 배포(HTTPS)에서는 true, 로컬(HTTP)에서는 false
                .sameSite(cookieProperties.getSameSite()) // 크로스오리진 프론트 대응: 배포는 None, 로컬은 Lax
                .path("/")
                .maxAge(3600)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        return ResponseEntity.ok().build();
    }

    @Operation(summary = "고객 전체 목록 조회", description = "등록된 모든 고객의 기본 정보(ID, 이름, 보유 포인트)를 조회합니다.")
    @GetMapping
    public ResponseEntity<List<CustomerResponseDto>> getAll() {
        return ResponseEntity.ok(customerService.getCustomers());
    }

    @Operation(summary = "고객 상세 조회", description = "customerId로 고객 상세 정보를 조회합니다. 보유 포인트와 현재 주문 중인 상품 목록을 함께 반환합니다.")
    @GetMapping("/{customerId}")
    public ResponseEntity<CustomerDetailResponseDto> getDetail(
            @Parameter(description = "조회할 고객의 ID", example = "skala01") @PathVariable String customerId) {
        return ResponseEntity.ok(customerService.getCustomerDetail(customerId));
    }

    @Operation(summary = "이름으로 고객 조회", description = "customerName으로 일치하는 고객 목록을 조회합니다. 동명이인이 있을 수 있어 리스트로 반환합니다.")
    @GetMapping("/name/{customerName}")
    public ResponseEntity<List<CustomerResponseDto>> getByName(
            @Parameter(description = "조회할 고객 이름", example = "홍길동") @PathVariable String customerName) {
        return ResponseEntity.ok(customerService.getCustomersByName(customerName));
    }

    @Operation(summary = "고객 정보 수정", description = "고객 이름 등 기본 정보를 수정합니다. (비밀번호/포인트는 별도 API에서 처리)")
    @PutMapping
    public ResponseEntity<CustomerResponseDto> update(@RequestBody @Valid CustomerUpdateRequestDto request) {
        return ResponseEntity.ok(customerService.updateCustomer(request));
    }

    @Operation(summary = "고객 삭제", description = "customerId에 해당하는 고객을 삭제합니다. 연관된 주문상품도 함께 삭제됩니다(cascade).")
    @DeleteMapping("/{customerId}")
    public ResponseEntity<Void> delete(
            @Parameter(description = "삭제할 고객의 ID", example = "skala01") @PathVariable String customerId) {
        customerService.deleteCustomer(customerId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "고객 주문 상품 목록 조회", description = "customerId가 현재 주문(보유) 중인 상품 목록을 상품명·수량·최근 주문시각과 함께 조회합니다.")
    @GetMapping("/{customerId}/products")
    public ResponseEntity<List<CustomerProductDto>> getProducts(
            @Parameter(description = "조회할 고객의 ID", example = "skala01") @PathVariable String customerId) {
        return ResponseEntity.ok(customerService.getCustomerProducts(customerId));
    }
}