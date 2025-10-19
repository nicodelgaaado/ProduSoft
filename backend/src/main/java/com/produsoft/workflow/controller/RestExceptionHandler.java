package com.produsoft.workflow.controller;

import com.produsoft.workflow.exception.AiClientException;
import com.produsoft.workflow.exception.InvalidStageActionException;
import com.produsoft.workflow.exception.NotFoundException;
import java.time.Instant;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

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
                "message", ex.getMessage(),
                "status", HttpStatus.BAD_GATEWAY.value()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(Map.of(
                "timestamp", Instant.now(),
                "message", ex.getMessage(),
                "status", HttpStatus.INTERNAL_SERVER_ERROR.value()));
    }
}
