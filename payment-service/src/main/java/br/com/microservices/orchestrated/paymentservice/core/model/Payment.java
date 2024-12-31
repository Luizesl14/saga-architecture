package br.com.microservices.orchestrated.paymentservice.core.model;


import br.com.microservices.orchestrated.paymentservice.core.enums.EPaymentStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "payment")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String orderId;

    @Column(nullable = false)
    private String transactionId;

    private int totalItems;

    private double totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EPaymentStatus status;

    @Column(nullable = false)
    private LocalDateTime createTime;

    @Column(nullable = false)
    private LocalDateTime updateTime;

    @PrePersist
    public void prePersist() {
        var now = LocalDateTime.now();
        this.updateTime = now;
        this.createTime = now;
        this.status = EPaymentStatus.PENDING;
    }

    @PreUpdate
    public void preUpdate() {
        this.updateTime = LocalDateTime.now();
    }

}
