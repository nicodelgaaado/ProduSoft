package com.produsoft.workflow.controller;

import com.produsoft.workflow.exception.AiClientException;
import com.produsoft.workflow.exception.InvalidStageActionException;
import com.produsoft.workflow.exception.NotFoundException;
import java.time.Instant;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class RestExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(NotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(Map.of(
                "timestamp", Instant.now(),
                "message", ex.getMessage(),
                "status", HttpStatus.NOT_FOUND.value()));
    }

    @ExceptionHandler(InvalidStageActionException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidAction(InvalidStageActionException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(Map.of(
                "timestamp", Instant.now(),
                "message", ex.getMessage(),
                "status", HttpStatus.BAD_REQUEST.value()));
    }

    @ExceptionHandler(AiClientException.class)
    public ResponseEntity<Map<String, Object>> handleAiClient(AiClientException ex) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
            .body(Map.of(
                "timestamp", Instant.now(),
                "message", "AI provider request failed.",
                "status", HttpStatus.BAD_GATEWAY.value()));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatus(ResponseStatusException ex) {
        String message = ex.getReason() != null ? ex.getReason() : ex.getStatusCode().toString();
        return ResponseEntity.status(ex.getStatusCode())
            .body(Map.of(
                "timestamp", Instant.now(),
                "message", message,
                "status", ex.getStatusCode().value()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex) {
        HttpStatus status = ex instanceof RestClientResponseException restEx
            ? HttpStatus.resolve(restEx.getStatusCode().value())
            : HttpStatus.INTERNAL_SERVER_ERROR;
        if (status == null || status.is5xxServerError()) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        return ResponseEntity.status(status)
            .body(Map.of(
                "timestamp", Instant.now(),
                "message", status.is4xxClientError()
                    ? "Request could not be processed."
                    : "Internal server error.",
                "status", status.value()));
    }
}
