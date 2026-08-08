package com.skala.shop.data.entity;

import com.skala.shop.exception.BusinessException;
import com.skala.shop.exception.ErrorCode;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "product")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private Long price;

    @Column
    private Long costPrice; // nullable - 매입원가, 없을 수 있음

    @Column(nullable = false)
    @Builder.Default
    private Integer stock = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id")
    private Supplier supplier; // nullable

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category; // nullable

    public void decreaseStock(int quantity) {
        if (this.stock < quantity) {
            throw new BusinessException(ErrorCode.OUT_OF_STOCK);
        }
        this.stock -= quantity;
    }

    public void increaseStock(int quantity) {
        this.stock += quantity;
    }

    public void update(String name, Long price, Long costPrice, Integer stock,
            Supplier supplier, Category category) {
        this.name = name;
        this.price = price;
        this.costPrice = costPrice;
        this.stock = stock;
        this.supplier = supplier;
        this.category = category;
    }
}