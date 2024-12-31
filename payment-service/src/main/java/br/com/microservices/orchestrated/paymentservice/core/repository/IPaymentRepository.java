package br.com.microservices.orchestrated.paymentservice.core.repository;

import br.com.microservices.orchestrated.paymentservice.core.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IPaymentRepository extends JpaRepository<Payment, Long> {


    @Query("SELECT COUNT(p) > 0 FROM Payment p WHERE p.orderId = :orderIdParam AND p.transactionId = :transactionIdParam")
    Boolean existsByOrderIdAndTransactionId(@Param("orderIdParam") String orderIdParam,
                                            @Param("transactionIdParam") String transactionIdParam);


    @Query("FROM Payment p WHERE p.orderId= :orderIdParam AND p.transactionId = :transactionIdParam")
    Optional<Payment> findByOrderIdAndTransactionId(@Param("orderIdParam") String orderIdParam,
                                                       @Param("transactionIdParam") String transactionId);


}
