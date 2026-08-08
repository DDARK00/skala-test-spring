package com.skala.shop.service;

import com.skala.shop.data.dto.CancelRequestDto;
import com.skala.shop.data.dto.OrderRequestDto;
import com.skala.shop.data.dto.OrderResponseDto;
import com.skala.shop.data.entity.*;
import com.skala.shop.exception.BusinessException;
import com.skala.shop.exception.ErrorCode;
import com.skala.shop.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * OrderServiceImpl 을 Repository 전부 Mock으로 격리해서 비즈니스 로직 분기만 검증한다.
 * bash 통합테스트가 이미 같은 시나리오를 커버하지만, 여기서는 예외 타입/코드를
 * 코드 레벨로 고정해서 회귀를 방지하는 것이 목적이다.
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock private CustomerRepository customerRepository;
    @Mock private ProductRepository productRepository;
    @Mock private CustomerHoldingRepository customerHoldingRepository;
    @Mock private OrderLogRepository orderLogRepository;
    @Mock private PointHistoryRepository pointHistoryRepository;
    @Mock private StockHistoryRepository stockHistoryRepository;

    @InjectMocks
    private OrderServiceImpl orderService;

    private Customer customer;
    private Product product;

    @BeforeEach
    void setUp() {
        customer = Customer.builder()
                .customerId("skala01").customerPassword("pw").customerName("테스트")
                .point(50000L).build();
        product = Product.builder()
                .name("상품").price(10000L).stock(10).build();
    }

    @Test
    void order_정상_주문시_포인트가_차감되고_보유수량이_생성된다() {
        // given
        when(customerRepository.findByCustomerIdForUpdate("skala01")).thenReturn(Optional.of(customer));
        when(productRepository.findWithLockById(1L)).thenReturn(Optional.of(product));
        when(customerHoldingRepository.findByCustomerAndProductForUpdate(customer, product))
                .thenReturn(Optional.empty());

        OrderRequestDto request = new OrderRequestDto(1L, 2);

        // when
        OrderResponseDto response = orderService.order("skala01", request);

        // then
        assertThat(response.remainingPoint()).isEqualTo(30000L); // 50000 - 10000*2
        assertThat(product.getStock()).isEqualTo(8); // 10 - 2
        verify(customerHoldingRepository).save(any(CustomerHolding.class));
        verify(stockHistoryRepository).save(any(StockHistory.class));
        verify(pointHistoryRepository).save(any(PointHistory.class));
        verify(orderLogRepository).save(any(OrderLog.class));
    }

    @Test
    void order_재주문시_기존_보유수량에_누적된다() {
        // given
        CustomerHolding existing = CustomerHolding.builder()
                .customer(customer).product(product).quantity(3).orderedAt(LocalDateTime.now())
                .build();

        when(customerRepository.findByCustomerIdForUpdate("skala01")).thenReturn(Optional.of(customer));
        when(productRepository.findWithLockById(1L)).thenReturn(Optional.of(product));
        when(customerHoldingRepository.findByCustomerAndProductForUpdate(customer, product))
                .thenReturn(Optional.of(existing));

        // when
        orderService.order("skala01", new OrderRequestDto(1L, 2));

        // then
        assertThat(existing.getQuantity()).isEqualTo(5); // 3 + 2
        verify(customerHoldingRepository, never()).save(any()); // 신규 저장이 아니라 기존 것 갱신
    }

    @Test
    void order_포인트_부족시_INSUFFICIENT_FUNDS_예외() {
        // given
        Customer poorCustomer = Customer.builder()
                .customerId("skala03").customerPassword("pw").customerName("가난한고객")
                .point(5000L).build();

        when(customerRepository.findByCustomerIdForUpdate("skala03")).thenReturn(Optional.of(poorCustomer));
        when(productRepository.findWithLockById(1L)).thenReturn(Optional.of(product));

        // when & then
        assertThatThrownBy(() -> orderService.order("skala03", new OrderRequestDto(1L, 1)))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.INSUFFICIENT_FUNDS));

        // 실패 시 어떤 이력도 저장되면 안 됨 (부분 반영 방지)
        verify(orderLogRepository, never()).save(any());
        verify(pointHistoryRepository, never()).save(any());
    }

    @Test
    void order_재고_부족시_OUT_OF_STOCK_예외() {
        // given
        Product lowStock = Product.builder().name("품절임박").price(1000L).stock(1).build();

        when(customerRepository.findByCustomerIdForUpdate("skala01")).thenReturn(Optional.of(customer));
        when(productRepository.findWithLockById(1L)).thenReturn(Optional.of(lowStock));

        // when & then
        assertThatThrownBy(() -> orderService.order("skala01", new OrderRequestDto(1L, 5)))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.OUT_OF_STOCK));
    }

    @Test
    void order_존재하지_않는_고객이면_DATA_NOT_FOUND_예외() {
        when(customerRepository.findByCustomerIdForUpdate("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.order("ghost", new OrderRequestDto(1L, 1)))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.DATA_NOT_FOUND));
    }

    @Test
    void cancel_보유수량보다_많은_취소요청시_PARAMETER_EXCEPTION() {
        // given
        CustomerHolding holding = CustomerHolding.builder()
                .customer(customer).product(product).quantity(2).orderedAt(LocalDateTime.now())
                .build();

        when(customerRepository.findByCustomerIdForUpdate("skala01")).thenReturn(Optional.of(customer));
        when(productRepository.findWithLockById(1L)).thenReturn(Optional.of(product));
        when(customerHoldingRepository.findByCustomerAndProductForUpdate(customer, product))
                .thenReturn(Optional.of(holding));

        // when & then - 보유량(2)보다 많은 3개 취소 시도
        assertThatThrownBy(() -> orderService.cancel("skala01", new CancelRequestDto(1L, 3)))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.PARAMETER_EXCEPTION));
    }

    @Test
    void cancel_전량_취소하면_holding이_삭제된다() {
        // given
        CustomerHolding holding = CustomerHolding.builder()
                .customer(customer).product(product).quantity(2).orderedAt(LocalDateTime.now())
                .build();

        when(customerRepository.findByCustomerIdForUpdate("skala01")).thenReturn(Optional.of(customer));
        when(productRepository.findWithLockById(1L)).thenReturn(Optional.of(product));
        when(customerHoldingRepository.findByCustomerAndProductForUpdate(customer, product))
                .thenReturn(Optional.of(holding));

        // when
        OrderResponseDto response = orderService.cancel("skala01", new CancelRequestDto(1L, 2));

        // then
        assertThat(response.remainingPoint()).isEqualTo(70000L); // 50000 + 10000*2
        assertThat(product.getStock()).isEqualTo(12); // 10 + 2
        verify(customerHoldingRepository).delete(holding);
    }

    @Test
    void cancel_일부만_취소하면_holding은_유지되고_수량만_감소() {
        // given
        CustomerHolding holding = CustomerHolding.builder()
                .customer(customer).product(product).quantity(5).orderedAt(LocalDateTime.now())
                .build();

        when(customerRepository.findByCustomerIdForUpdate("skala01")).thenReturn(Optional.of(customer));
        when(productRepository.findWithLockById(1L)).thenReturn(Optional.of(product));
        when(customerHoldingRepository.findByCustomerAndProductForUpdate(customer, product))
                .thenReturn(Optional.of(holding));

        // when
        orderService.cancel("skala01", new CancelRequestDto(1L, 2));

        // then
        assertThat(holding.getQuantity()).isEqualTo(3);
        verify(customerHoldingRepository, never()).delete(any());
    }
}
