package br.com.microservices.orchestrated.orderservice.core.controller;


import br.com.microservices.orchestrated.orderservice.core.document.Event;
import br.com.microservices.orchestrated.orderservice.core.document.Order;
import br.com.microservices.orchestrated.orderservice.core.dto.EventFilters;
import br.com.microservices.orchestrated.orderservice.core.dto.OrderRequest;
import br.com.microservices.orchestrated.orderservice.core.service.EventService;
import br.com.microservices.orchestrated.orderservice.core.service.OrderService;
import lombok.AllArgsConstructor;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping("/api/event")
public class EventController {

    private final EventService eventService;

    @GetMapping
    public ResponseEntity<Event> findByFilters(EventFilters eventFilters){
        return ResponseEntity.ok(this.eventService.findByFilters(eventFilters));
    }

    public ResponseEntity<List<Event>> findAll(){
        return ResponseEntity.ok(this.eventService.findAll());
    }
}
