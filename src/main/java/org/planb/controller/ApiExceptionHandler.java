package org.planb.controller;

import org.planb.dto.MessageResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class)
    ResponseEntity<MessageResponse> validation(org.springframework.web.bind.MethodArgumentNotValidException exception) {
        return ResponseEntity.badRequest().body(new MessageResponse(
                exception.getBindingResult().getFieldErrors().getFirst().getDefaultMessage()));
    }

    @ExceptionHandler(SecurityException.class)
    ResponseEntity<MessageResponse> forbidden(SecurityException exception) {
        return ResponseEntity.status(403).body(new MessageResponse("no permission"));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<MessageResponse> invalid(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(new MessageResponse(exception.getMessage()));
    }

    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<MessageResponse> status(ResponseStatusException exception) {
        return ResponseEntity.status(exception.getStatusCode())
                .body(new MessageResponse(exception.getReason()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<MessageResponse> conflict(DataIntegrityViolationException exception) {
        return ResponseEntity.status(409).body(new MessageResponse("data conflict"));
    }
}
