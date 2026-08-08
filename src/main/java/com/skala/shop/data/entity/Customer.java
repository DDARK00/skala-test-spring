package com.skala.shop.data.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.ArrayList;
import java.util.List;

import com.skala.shop.exception.BusinessException;
import com.skala.shop.exception.ErrorCode;

@Entity
@Table(name = "customer")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String customerId;

    @Column(nullable = false)
    private String customerPassword;

    @Column(nullable = false, length = 50)
    private String customerName;

    @Column(nullable = false)
    @Builder.Default
    private Long point = 0L;

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CustomerHolding> holdings = new ArrayList<>();

    // 포인트 차감/환급은 Service 계층에서 처리하되,
    // 엔티티에 최소한의 상태 변경 메서드는 두는 편이 응집도가 높음
    public void deductPoint(Long amount) {
        if (this.point < amount) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_FUNDS);
        }
        this.point -= amount;
    }

    public void refundPoint(Long amount) {
        this.point += amount;
    }

    public void updateName(String customerName) {
        this.customerName = customerName;
    }
}