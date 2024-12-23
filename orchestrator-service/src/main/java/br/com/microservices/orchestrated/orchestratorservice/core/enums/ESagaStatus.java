package br.com.microservices.orchestrated.orchestratorservice.core.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ESagaStatus {

    SUCCESS,
    ROLLBACK_PENDING,
    FAIL;
}
