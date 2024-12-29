package br.com.microservices.orchestrated.orderservice.core.repository;

import br.com.microservices.orchestrated.orderservice.core.document.Event;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IEventRepository extends MongoRepository<Event, String> {

    List<Event> findAllByOrderByCreateTimeDesc();
    Optional<Event> findTop1ByOrderIdOrderByCreateTimeDesc(String orderId);
    Optional<Event> findTop1ByTransactionIdOrderByCreateTimeDesc(String transactionId);
}
