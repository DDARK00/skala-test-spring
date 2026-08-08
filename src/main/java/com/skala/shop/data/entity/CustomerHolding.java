package com.skala.shop.data.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "customer_holding", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "customer_id", "product_id" })
})
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerHolding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private LocalDateTime orderedAt;

    public void increaseQuantity(int amount) {
        this.quantity += amount;
        this.orderedAt = LocalDateTime.now();
    }

    public void decreaseQuantity(int amount) {
        this.quantity -= amount;
    }
}