package com.skala.shop.service;

import com.skala.shop.data.dto.CancelRequestDto;
import com.skala.shop.data.dto.OrderRequestDto;
import com.skala.shop.data.dto.OrderResponseDto;
import com.skala.shop.data.entity.*;
import com.skala.shop.exception.BusinessException;
import com.skala.shop.exception.ErrorCode;
import com.skala.shop.repository.*;
import lombok.RequiredArgsConstructor;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

        private final CustomerRepository customerRepository;
        private final ProductRepository productRepository;
        private final CustomerHoldingRepository customerHoldingRepository;
        private final OrderLogRepository orderLogRepository;
        private final PointHistoryRepository pointHistoryRepository;
        private final StockHistoryRepository stockHistoryRepository;

        @Override
        @Transactional
        @Caching(evict = {
                        @CacheEvict(value = "products", key = "#request.productId"),
                        @CacheEvict(value = "productList", allEntries = true)
        })
        public OrderResponseDto order(String customerId, OrderRequestDto request) {
                // 락 순서: Customer -> Product -> CustomerHolding (order/cancel 양쪽 동일하게 유지, 데드락
                // 방지)
                Customer customer = customerRepository.findByCustomerIdForUpdate(customerId)
                                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND));
                Product product = productRepository.findWithLockById(request.productId())
                                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND));

                int quantity = request.quantity();
                long unitPrice = product.getPrice();
                long totalPrice = unitPrice * quantity;
                LocalDateTime now = LocalDateTime.now();

                // 1) 재고 확인 및 차감
                product.decreaseStock(quantity);

                // 2) 포인트 검증 및 차감
                customer.deductPoint(totalPrice);

                // 3) 보유 수량 갱신 (재주문 누적 / 신규 생성)
                CustomerHolding holding = customerHoldingRepository.findByCustomerAndProductForUpdate(customer, product)
                                .orElse(null);
                if (holding == null) {
                        holding = CustomerHolding.builder()
                                        .customer(customer).product(product)
                                        .quantity(quantity)
                                        .orderedAt(now)
                                        .build();
                        customerHoldingRepository.save(holding);
                } else {
                        holding.increaseQuantity(quantity);
                }

                // 4) 재고 이력
                stockHistoryRepository.save(StockHistory.builder()
                                .product(product)
                                .amount(-quantity)
                                .unitCost(null)
                                .reason(StockReason.SALE_OUT)
                                .createdAt(now)
                                .build());

                // 5) 포인트 이력
                pointHistoryRepository.save(PointHistory.builder()
                                .customer(customer)
                                .amount(-totalPrice)
                                .reason(PointReason.ORDER)
                                .createdAt(now)
                                .build());

                // 6) 주문 행위 이력
                orderLogRepository.save(OrderLog.builder()
                                .customer(customer).product(product)
                                .type(OrderLogType.ORDER)
                                .quantity(quantity)
                                .priceAtOrder(unitPrice)
                                .createdAt(now)
                                .build());

                return new OrderResponseDto(customer.getPoint());
        }

        @Override
        @Transactional
        @Caching(evict = {
                        @CacheEvict(value = "products", key = "#request.productId"),
                        @CacheEvict(value = "productList", allEntries = true)
        })
        public OrderResponseDto cancel(String customerId, CancelRequestDto request) {
                // 락 순서: Customer -> Product -> CustomerHolding (order()와 동일한 순서 유지)
                Customer customer = customerRepository.findByCustomerIdForUpdate(customerId)
                                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND));
                Product product = productRepository.findWithLockById(request.productId())
                                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND));

                CustomerHolding holding = customerHoldingRepository.findByCustomerAndProductForUpdate(customer, product)
                                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND));

                int quantity = request.quantity();
                if (holding.getQuantity() < quantity) {
                        throw new BusinessException(ErrorCode.PARAMETER_EXCEPTION);
                }

                long unitPrice = product.getPrice();
                long refundAmount = unitPrice * quantity;
                LocalDateTime now = LocalDateTime.now();

                // 1) 재고 복구
                product.increaseStock(quantity);

                // 2) 포인트 환급
                customer.refundPoint(refundAmount);

                // 3) 보유 수량 차감, 0이면 삭제
                holding.decreaseQuantity(quantity);
                if (holding.getQuantity() <= 0) {
                        customerHoldingRepository.delete(holding);
                }

                // 4) 재고 이력
                stockHistoryRepository.save(StockHistory.builder()
                                .product(product)
                                .amount(quantity)
                                .unitCost(null)
                                .reason(StockReason.CANCEL_RETURN)
                                .createdAt(now)
                                .build());

                // 5) 포인트 이력
                pointHistoryRepository.save(PointHistory.builder()
                                .customer(customer)
                                .amount(refundAmount)
                                .reason(PointReason.CANCEL)
                                .createdAt(now)
                                .build());

                // 6) 취소 행위 이력
                orderLogRepository.save(OrderLog.builder()
                                .customer(customer).product(product)
                                .type(OrderLogType.CANCEL)
                                .quantity(quantity)
                                .priceAtOrder(unitPrice)
                                .createdAt(now)
                                .build());

                return new OrderResponseDto(customer.getPoint());
        }
}