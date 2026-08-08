package com.skala.shop.service;

import java.util.List;

import com.skala.shop.data.dto.CustomerCreateRequestDto;
import com.skala.shop.data.dto.CustomerDetailResponseDto;
import com.skala.shop.data.dto.CustomerLoginRequestDto;
import com.skala.shop.data.dto.CustomerProductDto;
import com.skala.shop.data.dto.CustomerResponseDto;
import com.skala.shop.data.dto.CustomerUpdateRequestDto;

public interface CustomerService {
    CustomerResponseDto createCustomer(CustomerCreateRequestDto request);

    String login(CustomerLoginRequestDto request); // 반환값: JWT 토큰

    List<CustomerResponseDto> getCustomers();

    CustomerDetailResponseDto getCustomerDetail(String customerId);

    List<CustomerResponseDto> getCustomersByName(String customerName);

    CustomerResponseDto updateCustomer(CustomerUpdateRequestDto request);

    void deleteCustomer(String customerId);

    List<CustomerProductDto> getCustomerProducts(String customerId);
}