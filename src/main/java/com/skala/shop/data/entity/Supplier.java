package com.skala.shop.data.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "supplier")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Supplier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;
}