package com.skala.shop.data.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "stock_history")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private Integer amount; // 증감 수량 (+입고, -판매)

    private Long unitCost; // nullable - 입고(PURCHASE_IN) 건에만 매입단가 기록

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StockReason reason;

    @Column(nullable = false)
    private LocalDateTime createdAt;
}