package com.skala.shop.repository;

import com.skala.shop.data.entity.IdempotencyKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, Long> {
    Optional<IdempotencyKey> findByKeyValueAndEndpoint(String keyValue, String endpoint);
}