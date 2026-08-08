package com.skala.shop.data.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "idempotency_key", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"key_value", "endpoint"})
})
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IdempotencyKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "key_value", nullable = false, length = 100)
    private String keyValue;

    @Column(nullable = false, length = 100)
    private String endpoint; // "/api/customers/order" 등 - 같은 키라도 엔드포인트가 다르면 별개로 취급

    @Column(nullable = false, columnDefinition = "CLOB")
    private String responseBody; // 최초 처리 결과를 그대로 저장, 재요청 시 그대로 반환

    @Column(nullable = false)
    private Integer responseStatus;

    @Column(nullable = false)
    private LocalDateTime createdAt;
}