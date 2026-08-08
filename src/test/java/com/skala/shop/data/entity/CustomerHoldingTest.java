package com.skala.shop.data.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class CustomerHoldingTest {

    @Test
    void increaseQuantity_수량증가와_동시에_orderedAt이_갱신된다() {
        LocalDateTime original = LocalDateTime.of(2026, 1, 1, 0, 0);
        CustomerHolding holding = CustomerHolding.builder()
                .quantity(2).orderedAt(original).build();

        holding.increaseQuantity(3);

        assertThat(holding.getQuantity()).isEqualTo(5);
        assertThat(holding.getOrderedAt()).isAfter(original);
    }

    @Test
    void decreaseQuantity_orderedAt은_변경되지_않는다() {
        // 설계 의도: 취소는 "마지막 주문 시각" 의미를 훼손하면 안 됨
        LocalDateTime original = LocalDateTime.of(2026, 1, 1, 0, 0);
        CustomerHolding holding = CustomerHolding.builder()
                .quantity(5).orderedAt(original).build();

        holding.decreaseQuantity(2);

        assertThat(holding.getQuantity()).isEqualTo(3);
        assertThat(holding.getOrderedAt()).isEqualTo(original);
    }

    @Test
    void decreaseQuantity_전량_취소하면_0이_된다() {
        CustomerHolding holding = CustomerHolding.builder()
                .quantity(4).orderedAt(LocalDateTime.now()).build();

        holding.decreaseQuantity(4);

        assertThat(holding.getQuantity()).isEqualTo(0);
    }
}
