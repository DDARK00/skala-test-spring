package com.skala.shop.data.entity;

import com.skala.shop.exception.BusinessException;
import com.skala.shop.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CustomerTest {

    private Customer newCustomer(long point) {
        return Customer.builder()
                .customerId("u1").customerPassword("pw").customerName("테스트")
                .point(point).build();
    }

    @Test
    void deductPoint_보유포인트와_정확히_같으면_0원이_된다() {
        // 경계값: 딱 떨어지는 케이스가 "부족"으로 오판되지 않아야 함
        Customer customer = newCustomer(10000L);

        customer.deductPoint(10000L);

        assertThat(customer.getPoint()).isEqualTo(0L);
    }

    @Test
    void deductPoint_1원이라도_부족하면_예외() {
        Customer customer = newCustomer(9999L);

        assertThatThrownBy(() -> customer.deductPoint(10000L))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.INSUFFICIENT_FUNDS));
    }

    @Test
    void deductPoint_실패시_포인트는_변경되지_않는다() {
        // 예외가 나도 상태가 부분적으로 바뀌면 안 됨 (원자성 확인)
        Customer customer = newCustomer(5000L);

        assertThatThrownBy(() -> customer.deductPoint(10000L)).isInstanceOf(BusinessException.class);

        assertThat(customer.getPoint()).isEqualTo(5000L);
    }

    @Test
    void refundPoint_정상적으로_증가한다() {
        Customer customer = newCustomer(1000L);

        customer.refundPoint(500L);

        assertThat(customer.getPoint()).isEqualTo(1500L);
    }
}
