package com.skala.shop.repository;

import com.skala.shop.data.entity.Customer;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findByCustomerId(String customerId);

    List<Customer> findByCustomerName(String customerName);

    boolean existsByCustomerId(String customerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Customer c where c.customerId = :customerId")
    Optional<Customer> findByCustomerIdForUpdate(@Param("customerId") String customerId);
}