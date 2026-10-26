package org.planb.exception;

import java.util.stream.Collectors;

import org.planb.dto.MessageResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice 
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<MessageResponse> handleIllegalArgumentException( IllegalArgumentException exception){
        MessageResponse response = new MessageResponse(exception.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<MessageResponse> handleSecurityException( SecurityException exception){
        MessageResponse response = new MessageResponse(exception.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<MessageResponse> handleValidationException(MethodArgumentNotValidException exception){
        String message = exception.getBindingResult().getFieldErrors().stream().map(error -> error.getField()
                                                                                                        + ": "
                                                                                                        + error.getDefaultMessage()).collect(Collectors.joining("; "));

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new MessageResponse(message));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<MessageResponse> handleUnreadableMessage(HttpMessageNotReadableException exception){

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new MessageResponse("invalid JSON or enum value"));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<MessageResponse> handleIllegalStateException(IllegalStateException exception){

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new MessageResponse(exception.getMessage()));
    }
}
