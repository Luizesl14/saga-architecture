package br.com.microservices.orchestrated.orderservice.core.service;

import br.com.microservices.orchestrated.orderservice.core.document.Event;
import br.com.microservices.orchestrated.orderservice.core.document.Order;
import br.com.microservices.orchestrated.orderservice.core.dto.OrderRequest;
import br.com.microservices.orchestrated.orderservice.core.producer.SagaProducer;
import br.com.microservices.orchestrated.orderservice.core.repository.IOrderRepository;
import br.com.microservices.orchestrated.orderservice.core.utils.JsonUtil;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@AllArgsConstructor
public class OrderService {

    private static final String TRANSACTION_ID_PATTERN = "%s_%s";

    private final IOrderRepository repository;
    private final SagaProducer producer;
    private final JsonUtil jsonUtil;
    private final EventService eventService;

    public Order createOrder(OrderRequest orderRequest) {
        var order = Order.builder()
                .orderProducts(orderRequest.getProducts())
                .createTime(LocalDateTime.now())
                .transactionId(String.format(TRANSACTION_ID_PATTERN, Instant.now().toEpochMilli(), UUID.randomUUID()))
                .build();

        this.repository.save(order);
        this.producer.sendEvent(this.jsonUtil.toJson(this.createPayload(order)));
        return order;

    }
    private Event createPayload(Order order){
        var event = Event.builder()
                .orderId(order.getId())
                .transactionId(order.getTransactionId())
                .payload(order)
                .createTime(LocalDateTime.now())
                .build();
        this.eventService.save(event);
        return event;
    }
}
