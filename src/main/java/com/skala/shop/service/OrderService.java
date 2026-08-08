package com.skala.shop.service;

import com.skala.shop.data.dto.CancelRequestDto;
import com.skala.shop.data.dto.OrderRequestDto;
import com.skala.shop.data.dto.OrderResponseDto;

public interface OrderService {
    OrderResponseDto order(String customerId, OrderRequestDto request);

    OrderResponseDto cancel(String customerId, CancelRequestDto request);
}