package br.com.microservices.orchestrated.inventoryservice.core.service;

import br.com.microservices.orchestrated.inventoryservice.config.exception.ValidationException;
import br.com.microservices.orchestrated.inventoryservice.core.dto.Event;
import br.com.microservices.orchestrated.inventoryservice.core.dto.History;
import br.com.microservices.orchestrated.inventoryservice.core.dto.Order;
import br.com.microservices.orchestrated.inventoryservice.core.dto.OrderProduct;
import br.com.microservices.orchestrated.inventoryservice.core.model.Inventory;
import br.com.microservices.orchestrated.inventoryservice.core.model.OrderInventory;
import br.com.microservices.orchestrated.inventoryservice.core.producer.KafkaProducer;
import br.com.microservices.orchestrated.inventoryservice.core.repository.IInventoryRepository;
import br.com.microservices.orchestrated.inventoryservice.core.repository.IOrderInventoryRepository;
import br.com.microservices.orchestrated.inventoryservice.core.utils.JsonUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import static br.com.microservices.orchestrated.inventoryservice.core.enums.ESagaStatus.*;

@Slf4j
@Service
@AllArgsConstructor
public class InventoryService {

    private static final String CURRENT_SOURCE = "INVENTORY_SERVICE";

    private final JsonUtil jsonUtil;
    private final KafkaProducer producer;
    private final IInventoryRepository inventoryRepository;
    private final IOrderInventoryRepository orderInventoryRepository;


    public void updateInventory(Event event) {
        try {
            this.checkCurrentValidation(event);
            this.createOrderInventory(event);
            this.updateInventory(event.getPayload());
            this.handleSuccess(event);
        } catch (Exception e) {
            log.error("Error trying to update inventory", e);
            this.handleFailCurrentNotExecuted(event, e.getMessage());
        }

        this.producer.sendEvent(this.jsonUtil.toJson(event));

    }

    private void handleSuccess(Event event) {
        event.setStatus(SUCCESS);
        event.setSource(CURRENT_SOURCE);
        this.addHistory(event, "Inventory are validated successfully!");
    }

    private void handleFailCurrentNotExecuted(Event event, String message) {
        event.setStatus(ROLLBACK_PENDING);
        event.setSource(CURRENT_SOURCE);
        addHistory(event, "Fail to update inventory: " + message);
    }

    public void rollbackInventory(Event event) {
        event.setStatus(FAIL);
        event.setSource(CURRENT_SOURCE);
        try {
            this.returnInventoryToPreviousValues(event);
            addHistory(event, "Inventory rollback completed successfully!");
        }catch (Exception e) {
            addHistory(event, "Rollback failed with error: ".concat(e.getMessage()));
        }
        this.producer.sendEvent(this.jsonUtil.toJson(event));
    }

    public void returnInventoryToPreviousValues(Event event) {
        this.orderInventoryRepository.findByOrderIdAndTransactionId(event.getPayload().getId(), event.getTransactionId())
                .forEach(orderInventory -> {
                    var inventory = orderInventory.getInventory();
                    inventory.setAvailable(orderInventory.getOldQuantity());
                    this.inventoryRepository.save(inventory);
                    log.info("Inventory returned to the previous values: {}", inventory);
                });
    }


    public void checkCurrentValidation(Event event) throws Exception {
        if(this.orderInventoryRepository.existsByOrderIdAndTransactionId(
                event.getPayload().getId(),
                event.getPayload().getTransactionId())) {
            throw new Exception("There's another transactionId for this validation");

        }
    }

    public void createOrderInventory(Event event) {
        event.getPayload()
                .getOrderProducts()
                .forEach(p->{
                    var inventory = this.findInventoryByProductCode(p.getProduct().getCode());
                    var orderInventory = this.createOrderInventory(event, p, inventory);
                    this.orderInventoryRepository.save(orderInventory);
                });
    }

    private OrderInventory createOrderInventory(Event event, OrderProduct orderProduct, Inventory inventory){
        return OrderInventory
                .builder()
                .inventory(inventory)
                .oldQuantity(inventory.getAvailable())
                .orderQuantity(orderProduct.getQuantity())
                .newQuantity(inventory.getAvailable() - orderProduct.getQuantity())
                .orderId(event.getPayload().getId())
                .transactionId(event.getTransactionId())
                .build();

    }

    private void updateInventory(Order order){
        order.getOrderProducts()
                .forEach(product->{
                    var inventory = findInventoryByProductCode(product.getProduct().getCode());
                    this.checkInventory(inventory.getAvailable(), product.getQuantity());
                    inventory.setAvailable(inventory.getAvailable() - product.getQuantity());
                    this.inventoryRepository.save(inventory);
                });
    }

    private void checkInventory(int available, int orderQuantity) {
        if(orderQuantity > available) {
            throw new ValidationException("Product is out of stock");
        }
    }

    private Inventory findInventoryByProductCode(String productCode) {
        return this.inventoryRepository.findByProductCode(productCode)
                .orElseThrow(()-> new ValidationException("Inventory product code not found"));
    }

    public void addHistory(Event event, String message) {
        var history = History.builder()
                .source(event.getSource())
                .status(event.getStatus())
                .message(message)
                .createTime(event.getCreateTime())
                .build();
        event.addToHistory(history);
    }
}
