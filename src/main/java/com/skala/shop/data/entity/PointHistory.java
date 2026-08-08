package com.skala.shop.data.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "point_history")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PointHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(nullable = false)
    private Long amount; // 증감액 (+충전/환급, -차감), 부호로 방향 표현

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PointReason reason;

    @Column(nullable = false)
    private LocalDateTime createdAt;
}