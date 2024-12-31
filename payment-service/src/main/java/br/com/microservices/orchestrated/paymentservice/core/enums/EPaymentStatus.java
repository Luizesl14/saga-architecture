package br.com.microservices.orchestrated.paymentservice.core.enums;

import lombok.Getter;

@Getter
public enum EPaymentStatus {
    PENDING,
    SUCCESS,
    REFUNDED
}
