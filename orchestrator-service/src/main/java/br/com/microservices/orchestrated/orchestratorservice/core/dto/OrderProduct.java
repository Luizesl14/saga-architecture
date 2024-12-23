package br.com.microservices.orchestrated.orchestratorservice.core.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderProduct {

    private Integer quantity;
    private Product product;
}
