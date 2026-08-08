package com.skala.shop.data.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "order_log")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderLogType type;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private Long priceAtOrder;

    @Column(nullable = false)
    private LocalDateTime createdAt;
}