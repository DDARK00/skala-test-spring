package com.skala.shop.repository;

import com.skala.shop.data.entity.Customer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @Param 누락 같은 named-parameter 쿼리 문법 오류를 API 호출 전에 잡기 위한 테스트.
 * (실제로 findByCustomerIdForUpdate 에서 @Param 누락으로 500이 났던 사례가 있었음)
 */
@DataJpaTest
@ActiveProfiles("test")
class CustomerRepositoryTest {

    @Autowired
    private CustomerRepository customerRepository;

    @Test
    void findByCustomerIdForUpdate_정상_조회된다() {
        // given
        Customer customer = Customer.builder()
                .customerId("testuser01")
                .customerPassword("encoded-pw")
                .customerName("테스트유저")
                .point(50000L)
                .build();
        customerRepository.save(customer);

        // when - 락 쿼리 실행 자체가 예외 없이 동작하는지가 핵심 검증 포인트
        Optional<Customer> found = customerRepository.findByCustomerIdForUpdate("testuser01");

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getCustomerId()).isEqualTo("testuser01");
        assertThat(found.get().getPoint()).isEqualTo(50000L);
    }

    @Test
    void findByCustomerIdForUpdate_존재하지_않으면_빈값() {
        Optional<Customer> found = customerRepository.findByCustomerIdForUpdate("no-such-user");
        assertThat(found).isEmpty();
    }
}
