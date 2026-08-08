package com.skala.shop.controller;

import com.skala.shop.config.JwtAuthenticationFilter;
import com.skala.shop.data.dto.CancelRequestDto;
import com.skala.shop.data.dto.OrderRequestDto;
import com.skala.shop.data.dto.OrderResponseDto;
import com.skala.shop.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Order", description = "주문/취소 API (로그인 필요)")
@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @Operation(summary = "상품 주문", description = "로그인한 고객이 상품을 주문합니다. 보유 포인트에서 (가격 x 수량)만큼 차감되며, "
            + "포인트가 부족하면 주문이 거부됩니다(INSUFFICIENT_FUNDS). "
            + "같은 상품을 재주문하면 기존 주문의 수량에 누적됩니다.")
    @PostMapping("/order")
    public ResponseEntity<OrderResponseDto> order(HttpServletRequest httpRequest,
            @RequestBody @Valid OrderRequestDto request) {
        String customerId = (String) httpRequest.getAttribute(JwtAuthenticationFilter.CUSTOMER_ID_ATTRIBUTE);
        return ResponseEntity.ok(orderService.order(customerId, request));
    }

    @Operation(summary = "주문 취소", description = "로그인한 고객이 주문한 상품을 취소합니다. 취소 수량만큼 포인트가 환급되며, "
            + "취소 후 남은 수량이 0이면 해당 주문상품 항목이 삭제됩니다.")
    @PostMapping("/cancel")
    public ResponseEntity<OrderResponseDto> cancel(HttpServletRequest httpRequest,
            @RequestBody @Valid CancelRequestDto request) {
        String customerId = (String) httpRequest.getAttribute(JwtAuthenticationFilter.CUSTOMER_ID_ATTRIBUTE);
        return ResponseEntity.ok(orderService.cancel(customerId, request));
    }
}