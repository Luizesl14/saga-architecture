package br.com.microservices.orchestrated.productvalidationservice.core.repository;

import br.com.microservices.orchestrated.productvalidationservice.core.model.Validation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IValidationRepository extends JpaRepository<Validation, Long> {



    @Query("SELECT COUNT(v) > 0 FROM Validation v WHERE v.orderId = :orderIdParam AND v.transactionId = :transactionIdParam")
    Boolean existsByOrderIdAndTransactionId(@Param("orderIdParam") String orderIdParam,
                                            @Param("transactionIdParam") String transactionIdParam);


    @Query("FROM Validation v WHERE v.orderId= :orderIdParam AND v.transactionId = :transactionIdParam")
    Optional<Validation> findByOrderIdAndTransactionId(@Param("orderIdParam") String orderIdParam,
                                                       @Param("transactionIdParam") String transactionId);
}
