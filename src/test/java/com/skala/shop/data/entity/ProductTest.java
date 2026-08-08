package com.skala.shop.data.entity;

import com.skala.shop.exception.BusinessException;
import com.skala.shop.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductTest {

    private Product newProduct(int stock) {
        return Product.builder()
                .name("상품").price(10000L).stock(stock).build();
    }

    @Test
    void decreaseStock_재고와_정확히_같은_수량이면_0이_된다() {
        Product product = newProduct(5);

        product.decreaseStock(5);

        assertThat(product.getStock()).isEqualTo(0);
    }

    @Test
    void decreaseStock_재고보다_많으면_예외() {
        Product product = newProduct(5);

        assertThatThrownBy(() -> product.decreaseStock(6))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.OUT_OF_STOCK));
    }

    @Test
    void decreaseStock_실패시_재고는_변경되지_않는다() {
        Product product = newProduct(3);

        assertThatThrownBy(() -> product.decreaseStock(4)).isInstanceOf(BusinessException.class);

        assertThat(product.getStock()).isEqualTo(3);
    }

    @Test
    void increaseStock_정상적으로_증가한다() {
        Product product = newProduct(10);

        product.increaseStock(5);

        assertThat(product.getStock()).isEqualTo(15);
    }
}
