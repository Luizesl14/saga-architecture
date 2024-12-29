package br.com.microservices.orchestrated.orderservice.core.service;


import br.com.microservices.orchestrated.orderservice.config.exception.ValidationException;
import br.com.microservices.orchestrated.orderservice.core.document.Event;
import br.com.microservices.orchestrated.orderservice.core.dto.EventFilters;
import br.com.microservices.orchestrated.orderservice.core.repository.IEventRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

import static org.springframework.util.ObjectUtils.isEmpty;


@Slf4j
@Service
@AllArgsConstructor
public class EventService {

    private final IEventRepository repository;

    public Event findByFilters(EventFilters eventFilters) {
        this.validateEmptyFilters(eventFilters);
        if (isEmpty(eventFilters.getOrderId())) {
            return this.findByOrderId(eventFilters.getTransactionId());
        }else {
            return this.findByTransactionId(eventFilters.getOrderId());
        }
    }

    public void validateEmptyFilters(EventFilters eventFilters) {
        if (isEmpty(eventFilters.getOrderId()) && isEmpty(eventFilters.getTransactionId())) {
            throw new ValidationException("Order Id and Transaction Id cannot be empty");
        }
    }

    public List<Event> findAll() {
        return repository.findAllByOrderByCreateTimeDesc();
    }

    public Event findByOrderId(String orderId) {
        return this.repository.findTop1ByOrderIdOrderByCreateTimeDesc(orderId)
                .orElseThrow(()-> new ValidationException("Event not found by orderId."));
    }
    public Event findByTransactionId(String transactionId) {
        return this.repository.findTop1ByTransactionIdOrderByCreateTimeDesc(transactionId)
                .orElseThrow(()-> new ValidationException("Event not found by transactionId."));
    }


    public void notifyEnding(Event event) {
        event.setOrderId(event.getOrderId());
        event.setCreateTime(event.getCreateTime());
        this.save(event);
        log.info("Order {} with saga notified! Transaction Id: {}", event.getOrderId(), event.getTransactionId());
    }
    public Event save(Event event) {
        return repository.save(event);
    }
}
