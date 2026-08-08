package com.skala.shop.repository;

import com.skala.shop.data.entity.Customer;
import com.skala.shop.data.entity.CustomerHolding;
import com.skala.shop.data.entity.Product;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class CustomerHoldingRepositoryTest {

    @Autowired
    private CustomerHoldingRepository customerHoldingRepository;
    @Autowired
    private CustomerRepository customerRepository;
    @Autowired
    private ProductRepository productRepository;

    @Test
    void findByCustomerAndProductForUpdate_정상_조회된다() {
        // given
        Customer customer = customerRepository.save(Customer.builder()
                .customerId("holder01").customerPassword("pw").customerName("보유자")
                .point(100000L).build());
        Product product = productRepository.save(Product.builder()
                .name("보유상품").price(5000L).stock(50).build());

        customerHoldingRepository.save(CustomerHolding.builder()
                .customer(customer).product(product)
                .quantity(3).orderedAt(LocalDateTime.now())
                .build());

        // when - :customer, :product 두 개의 named parameter가 정확히 바인딩되는지가 핵심
        Optional<CustomerHolding> found =
                customerHoldingRepository.findByCustomerAndProductForUpdate(customer, product);

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getQuantity()).isEqualTo(3);
    }
}
