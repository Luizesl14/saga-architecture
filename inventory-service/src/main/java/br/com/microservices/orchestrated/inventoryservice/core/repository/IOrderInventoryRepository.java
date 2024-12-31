package br.com.microservices.orchestrated.inventoryservice.core.repository;

import br.com.microservices.orchestrated.inventoryservice.core.model.OrderInventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IOrderInventoryRepository extends JpaRepository<OrderInventory, Long> {

    @Query("SELECT COUNT(oi) > 0 FROM OrderInventory oi WHERE oi.orderId = :orderIdParam AND oi.transactionId = :transactionIdParam")
    Boolean existsByOrderIdAndTransactionId(@Param("orderIdParam") String orderIdParam,
                                            @Param("transactionIdParam") String transactionIdParam);


    @Query("FROM OrderInventory oi WHERE oi.orderId= :orderIdParam AND oi.transactionId = :transactionIdParam")
    List<OrderInventory> findByOrderIdAndTransactionId(@Param("orderIdParam") String orderIdParam,
                                                       @Param("transactionIdParam") String transactionId);
}
