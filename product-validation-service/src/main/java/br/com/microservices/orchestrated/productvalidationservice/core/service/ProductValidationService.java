package br.com.microservices.orchestrated.productvalidationservice.core.service;

import br.com.microservices.orchestrated.productvalidationservice.config.exception.ValidationException;
import br.com.microservices.orchestrated.productvalidationservice.core.dto.Event;
import br.com.microservices.orchestrated.productvalidationservice.core.dto.History;
import br.com.microservices.orchestrated.productvalidationservice.core.dto.OrderProduct;
import br.com.microservices.orchestrated.productvalidationservice.core.model.Validation;
import br.com.microservices.orchestrated.productvalidationservice.core.producer.KafkaProducer;
import br.com.microservices.orchestrated.productvalidationservice.core.repository.IProductRepository;
import br.com.microservices.orchestrated.productvalidationservice.core.repository.IValidationRepository;
import br.com.microservices.orchestrated.productvalidationservice.core.utils.JsonUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import static br.com.microservices.orchestrated.productvalidationservice.core.enums.ESagaStatus.*;
import static org.springframework.util.ObjectUtils.isEmpty;


@Service
@Slf4j
@AllArgsConstructor
public class ProductValidationService {

    private static final String CURRENT_SOURCE = "PRODUCT_VALIDATION_SERVICE";

    private final JsonUtil jsonUtil;
    private final KafkaProducer producer;
    private final IProductRepository productRepository;
    private final IValidationRepository validationRepository;

    public void validateExistingProduct(Event event) {
        try{
            checkCurrentValidation(event);
            createValidation(event, true);
            handleSuccess(event);
        } catch (Exception e) {
            log.info("Error trying to validate product: ", e);
            handleFailCurrentNotExecuted(event, e.getMessage());
        }
        this.producer.sendEvent(this.jsonUtil.toJson(event));
    }

    private void validatedProductsProvided(Event event) {
        if(isEmpty(event.getPayload()) || isEmpty(event.getPayload().getOrderProducts())){
            throw new ValidationException("Product list is empty");
        }
        if(isEmpty(event.getPayload().getId()) || isEmpty(event.getPayload().getTransactionId())){
            throw new ValidationException("OrderId and TransactionID must be provided");
        }
    }

    private void checkCurrentValidation(Event event){
        this.validateExistingProduct(event);
        if(this.validationRepository
                .existsByOrderIdAndTransactionId(
                        event.getOrderId(),
                        event.getTransactionId())){
            throw new ValidationException("There's another transactionID for this validation");
        }
        event.getPayload().getOrderProducts().forEach(product->{
            this.validateProductInformed(product);
            this.validateExistingProduct(product.getProduct().getCode());
        });
    }

    public void validateProductInformed(OrderProduct product) {
        if(isEmpty(product.getProduct()) || isEmpty(product.getProduct().getCode())){
            throw new ValidationException("Product code must be provided");
        }
    }
    private void validateExistingProduct(String code) {
        if(!this.productRepository.existsByCode(code)){
            throw new ValidationException("Product code " + code + " does not exist");
        }
    }

    private void createValidation(Event event, Boolean success) {
        var validation = Validation.builder()
                .orderId(event.getPayload().getId())
                .transactionId(event.getPayload().getTransactionId())
                .success(success)
                .build();

        this.validationRepository.save(validation);
    }

    private void handleSuccess(Event event) {
        event.setStatus(SUCCESS);
        event.setSource(CURRENT_SOURCE);
        this.addHistory(event, "Products are validated successfully!");
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

    public void handleFailCurrentNotExecuted(Event event, String message) {
        event.setStatus(ROLLBACK_PENDING);
        event.setSource(CURRENT_SOURCE);
        this.addHistory(event, "Fail to validate products: " .concat(message));
    }

    public void rollbackEvent(Event event) {
        changeValidationToFail(event);
        event.setStatus(FAIL);
        event.setSource(CURRENT_SOURCE);
        this.addHistory(event, "Rollback executed on product validation");
        producer.sendEvent(this.jsonUtil.toJson(event));
    }

    private void changeValidationToFail(Event event) {
        this.validationRepository.findByOrderIdAndTransactionId(
                event.getPayload().getId(),
                event.getPayload().getTransactionId())
                .ifPresentOrElse(validation -> {
                    validation.setSuccess(false);
                    this.validationRepository.save(validation);
                }, ()-> createValidation(event, false) );
    }

}
