package org.planb.controller;

import jakarta.validation.Valid;
import org.planb.dto.LoginRequest;
import org.planb.dto.LoginResponse;
import org.planb.dto.MessageResponse;
import org.planb.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import java.util.Optional;


@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        Optional<LoginResponse> response = authService.login(request);
        if (response.isPresent()) {
            return ResponseEntity.ok(response.get());
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new MessageResponse("Invalid email or password"));
        }
    }

    // Stateless logout: clients discard the JWT. No server-side session exists.
    @DeleteMapping
    public ResponseEntity<Void> logout() {
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler (MethodArgumentNotValidException.class)
    public ResponseEntity<MessageResponse> handleValidationError(MethodArgumentNotValidException ex) {
        String errorMessage = ex.getBindingResult().getFieldErrors().get(0).getDefaultMessage();
        return ResponseEntity.badRequest().body(new MessageResponse(errorMessage));
    }
}
