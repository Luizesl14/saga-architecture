package br.com.microservices.orchestrated.paymentservice.config.exceptin;

public record ExceptionDetails(
        int status,
        String message
) {

}
