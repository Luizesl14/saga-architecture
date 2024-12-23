package br.com.microservices.orchestrated.inventoryservice.config.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class ExceptionGlobalHandler {

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ExceptionDetails> handleException(ValidationException e) {
       var details = new ExceptionDetails(HttpStatus.BAD_REQUEST.value(), e.getMessage());
       return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(details);
    }
}
