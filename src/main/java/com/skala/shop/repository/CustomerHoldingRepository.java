package com.skala.shop.repository;

import com.skala.shop.data.entity.Customer;
import com.skala.shop.data.entity.CustomerHolding;
import com.skala.shop.data.entity.Product;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CustomerHoldingRepository extends JpaRepository<CustomerHolding, Long> {

    Optional<CustomerHolding> findByCustomerAndProduct(Customer customer, Product product);

    List<CustomerHolding> findByCustomer(Customer customer);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select h from CustomerHolding h where h.customer = :customer and h.product = :product")
    Optional<CustomerHolding> findByCustomerAndProductForUpdate(@Param("customer") Customer customer,
                                                                 @Param("product") Product product);
}