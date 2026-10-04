package dev.example.legalvoice.web;

import dev.example.legalvoice.infra.InfraiException;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(InfraiException.class)
    ResponseEntity<Map<String, String>> handleInfrai(InfraiException error) {
        int status = error.status() >= 400 && error.status() < 500 ? error.status() : 502;
        return ResponseEntity.status(status).body(Map.of("code", error.code(), "message", error.getMessage()));
    }
}
