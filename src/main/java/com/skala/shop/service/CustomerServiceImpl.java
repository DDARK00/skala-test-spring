package com.skala.shop.service;

import com.skala.shop.config.CustomerPolicyProperties;
import com.skala.shop.data.dto.*;
import com.skala.shop.data.entity.Customer;
import com.skala.shop.data.entity.CustomerHolding;
import com.skala.shop.data.entity.PointHistory;
import com.skala.shop.data.entity.PointReason;
import com.skala.shop.exception.BusinessException;
import com.skala.shop.exception.ErrorCode;
import com.skala.shop.repository.CustomerRepository;
import com.skala.shop.repository.PointHistoryRepository;
import com.skala.shop.tools.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomerServiceImpl implements CustomerService {

    private final PointHistoryRepository pointHistoryRepository;
    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final CustomerPolicyProperties customerPolicyProperties;

    @Override
    @Transactional
    public CustomerResponseDto createCustomer(CustomerCreateRequestDto request) {
        if (customerRepository.existsByCustomerId(request.customerId())) {
            throw new BusinessException(ErrorCode.DUPLICATE_CUSTOMER_ID);
        }
        Long point = (request.point() != null)
                ? request.point()
                : customerPolicyProperties.getDefaultPoint();

        Customer customer = Customer.builder()
                .customerId(request.customerId())
                .customerPassword(passwordEncoder.encode(request.customerPassword()))
                .customerName(request.customerName())
                .point(point)
                .build();

        Customer saved = customerRepository.save(customer);

        // 회계원장 원칙: 최초 지급 포인트도 이력으로 남김
        pointHistoryRepository.save(PointHistory.builder()
                .customer(saved)
                .amount(point)
                .reason(PointReason.SIGNUP_BONUS)
                .createdAt(LocalDateTime.now())
                .build());

        return toResponseDto(saved);
    }

    @Override
    public String login(CustomerLoginRequestDto request) {
        Customer customer = customerRepository.findByCustomerId(request.customerId())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(request.customerPassword(), customer.getCustomerPassword())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        return jwtTokenProvider.generateToken(customer.getCustomerId());
    }

    @Override
    public List<CustomerResponseDto> getCustomers() {
        return customerRepository.findAll().stream()
                .map(this::toResponseDto)
                .toList();
    }

    @Override
    public CustomerDetailResponseDto getCustomerDetail(String customerId) {
        Customer customer = findCustomerOrThrow(customerId);

        List<CustomerProductDto> products = customer.getHoldings().stream()
                .map(this::toProductDto)
                .toList();

        return new CustomerDetailResponseDto(customer.getCustomerId(), customer.getPoint(), products);
    }

    @Override
    public List<CustomerResponseDto> getCustomersByName(String customerName) {
        return customerRepository.findByCustomerName(customerName).stream()
                .map(this::toResponseDto)
                .toList();
    }

    @Override
    @Transactional
    public CustomerResponseDto updateCustomer(CustomerUpdateRequestDto request) {
        Customer customer = findCustomerOrThrow(request.customerId());
        customer.updateName(request.customerName());
        return toResponseDto(customer);
    }

    @Override
    @Transactional
    public void deleteCustomer(String customerId) {
        Customer customer = findCustomerOrThrow(customerId);
        customerRepository.delete(customer);
    }

    @Override
    public List<CustomerProductDto> getCustomerProducts(String customerId) {
        Customer customer = findCustomerOrThrow(customerId);
        return customer.getHoldings().stream()
                .map(this::toProductDto)
                .toList();
    }

    private Customer findCustomerOrThrow(String customerId) {
        return customerRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND));
    }

    private CustomerResponseDto toResponseDto(Customer customer) {
        return new CustomerResponseDto(customer.getCustomerId(), customer.getCustomerName(), customer.getPoint());
    }

    private CustomerProductDto toProductDto(CustomerHolding item) {
        return new CustomerProductDto(
                item.getProduct().getId(),
                item.getProduct().getName(),
                item.getQuantity(),
                item.getOrderedAt());
    }
}