package org.planb.controller;

import org.planb.dto.MessageResponse;
import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MethodArgumentNotValidException;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<MessageResponse> validation(MethodArgumentNotValidException error) {
        return ResponseEntity.badRequest().body(new MessageResponse(
                error.getBindingResult().getFieldErrors().getFirst().getDefaultMessage()));
    }

    @ExceptionHandler(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class)
    ResponseEntity<MessageResponse> query(Exception error) {
        return ResponseEntity.badRequest().body(new MessageResponse("invalid path or query parameter"));
    }

    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    ResponseEntity<MessageResponse> missing(Exception error) {
        return ResponseEntity.status(404).body(new MessageResponse("resource not found"));
    }

    @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
    ResponseEntity<MessageResponse> method(Exception error) {
        return ResponseEntity.status(405).body(new MessageResponse("method not allowed"));
    }

    @ExceptionHandler(ApiException.class)
    ResponseEntity<MessageResponse> api(ApiException error) {
        return ResponseEntity.status(error.getStatus()).body(new MessageResponse(error.getMessage()));
    }

    @ExceptionHandler(SecurityException.class)
    ResponseEntity<MessageResponse> permission(SecurityException error) {
        return ResponseEntity.status(403).body(new MessageResponse("no permission"));
    }

    @ExceptionHandler({IllegalArgumentException.class, HttpMessageNotReadableException.class})
    ResponseEntity<MessageResponse> input(Exception error) {
        return ResponseEntity.badRequest().body(new MessageResponse(
                error instanceof HttpMessageNotReadableException ? "invalid JSON or enum value" : error.getMessage()));
    }

    @ExceptionHandler({DataAccessException.class, IllegalStateException.class})
    ResponseEntity<MessageResponse> internal(Exception error) {
        org.slf4j.LoggerFactory.getLogger(ApiExceptionHandler.class).error("API operation failed", error);
        return ResponseEntity.internalServerError().body(new MessageResponse("internal server error"));
    }
}
