package com.skala.shop.repository;

import com.skala.shop.data.entity.OrderLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderLogRepository extends JpaRepository<OrderLog, Long> {
}