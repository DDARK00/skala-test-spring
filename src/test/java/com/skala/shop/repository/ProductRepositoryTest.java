package com.skala.shop.repository;

import com.skala.shop.data.entity.Product;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class ProductRepositoryTest {

    @Autowired
    private ProductRepository productRepository;

    @Test
    void findWithLockById_정상_조회된다() {
        // given
        Product product = Product.builder()
                .name("테스트상품")
                .price(10000L)
                .stock(100)
                .build();
        Product saved = productRepository.save(product);

        // when
        Optional<Product> found = productRepository.findWithLockById(saved.getId());

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getStock()).isEqualTo(100);
    }

    @Test
    void findWithLockById_존재하지_않으면_빈값() {
        Optional<Product> found = productRepository.findWithLockById(9999L);
        assertThat(found).isEmpty();
    }
}
