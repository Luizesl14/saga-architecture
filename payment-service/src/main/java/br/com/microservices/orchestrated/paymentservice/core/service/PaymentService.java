package br.com.microservices.orchestrated.paymentservice.core.service;

import br.com.microservices.orchestrated.paymentservice.config.exceptin.ValidationException;
import br.com.microservices.orchestrated.paymentservice.core.dto.Event;
import br.com.microservices.orchestrated.paymentservice.core.dto.History;
import br.com.microservices.orchestrated.paymentservice.core.dto.OrderProduct;
import br.com.microservices.orchestrated.paymentservice.core.dto.Product;
import br.com.microservices.orchestrated.paymentservice.core.enums.EPaymentStatus;
import br.com.microservices.orchestrated.paymentservice.core.model.Payment;
import br.com.microservices.orchestrated.paymentservice.core.producer.KafkaProducer;
import br.com.microservices.orchestrated.paymentservice.core.repository.IPaymentRepository;
import br.com.microservices.orchestrated.paymentservice.core.utils.JsonUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import static br.com.microservices.orchestrated.paymentservice.core.enums.ESagaStatus.*;

@Slf4j
@Service
@AllArgsConstructor
public class PaymentService {

    private static final String CURRENT_SOURCE = "PAYMENT_SERVICE";
    private static final Double REDUCE_SUN_VALUE = 0.0;
    private static final Double MINIMUM_AMOUNT = 5.0;

    private final JsonUtil jsonUtil;
    private final KafkaProducer producer;
    private final IPaymentRepository paymentRepository;

    public void realizedPayment(Event event) {
        try{
            this.checkCurrentValidation(event);
            this.createPendingPayment(event);
            var payment = this.findPaymentByOrderIdAndTransacionId(event);
            this.validatedAmount(payment.getTotalAmount());
            this.changePayment(payment);
            this.handleSuccess(event);

        } catch (Exception e) {
           log.error("Error trying to make payment: ", e);
           this.handleFailCurrentNotExecuted(event, e.getMessage());
        }
        this.producer.sendEvent(this.jsonUtil.toJson(event));
    }

    public void checkCurrentValidation(Event event) throws Exception {
        if(this.paymentRepository.existsByOrderIdAndTransactionId(
                event.getPayload().getId(),
                event.getPayload().getTransactionId())) {
            throw new Exception("There's another transactionId for this validation");

        }
    }

    private void createPendingPayment(Event event) {

        var totalAmount = this.calculateTotalAmount(event);
        var totalItems = this.calculateTotalItems(event);

        var payment = Payment.builder()
                .orderId(event.getPayload().getId())
                .transactionId(event.getPayload().getTransactionId())
                .totalAmount(totalAmount)
                .totalItems(totalItems)
                .build();

        this.save(payment);
        this.setEventAmountItems(event, payment);

    }

    private Double calculateTotalAmount(Event event) {
        return event.getPayload()
                .getOrderProducts()
                .stream()
                .map(OrderProduct::getProduct)
                .map(Product::getUnitValue)
                .reduce(REDUCE_SUN_VALUE, Double::sum);

    }

    private Integer calculateTotalItems(Event event) {
        return event.getPayload()
                .getOrderProducts()
                .stream()
                .map(OrderProduct::getQuantity)
                .reduce(REDUCE_SUN_VALUE.intValue(), Integer::sum);

    }

    private void setEventAmountItems(Event event, Payment payment) {
        event.getPayload().setAmount(payment.getTotalAmount());
        event.getPayload().setItems(payment.getTotalItems());
    }


    private void handleSuccess(Event event) {
        event.setStatus(SUCCESS);
        event.setSource(CURRENT_SOURCE);
        this.addHistory(event, "Payment realized successfully!");
    }

    private void handleFailCurrentNotExecuted(Event event, String message) {
        event.setStatus(ROLLBACK_PENDING);
        event.setSource(CURRENT_SOURCE);
        addHistory(event, "Fail to realize payment: " + message);
    }

    public void realizeRefund(Event event) {
        event.setStatus(FAIL);
        event.setSource(CURRENT_SOURCE);
        try{
            this.changePaymentStatusRefund(event);
            addHistory(event, "Rollback executed on payment refund!");
        } catch (Exception e) {
            addHistory(event, "Rollback not executed on payment refund: " + e.getMessage());
        }
        this.producer.sendEvent(this.jsonUtil.toJson(event));
    }

    private void changePayment(Payment payment) {
        payment.setStatus(EPaymentStatus.SUCCESS);
        this.save(payment);
    }

    private void changePaymentStatusRefund(Event event) {
        var payment = this.findPaymentByOrderIdAndTransacionId(event);
        payment.setStatus(EPaymentStatus.REFUNDED);
        setEventAmountItems(event, payment);
        this.save(payment);
    }

    private void validatedAmount(Double amount) {
        if(amount <  MINIMUM_AMOUNT){
            throw new ValidationException("The minimum amount of payment is " + MINIMUM_AMOUNT);
        }
    }

    private Payment findPaymentByOrderIdAndTransacionId(Event event) {
        return this.paymentRepository
                .findByOrderIdAndTransactionId(event.getPayload().getId(), event.getPayload().getTransactionId())
                .orElseThrow(()-> new ValidationException("Payment not found by OrderId and TransactionId"));
    }

    private void save(Payment payment) {
        this.paymentRepository.save(payment);
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
