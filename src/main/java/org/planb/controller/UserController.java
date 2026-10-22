package org.planb.controller;

import org.planb.dto.CreateUserRequest;
import org.planb.dto.MessageResponse;
import org.planb.dto.UserResponse;
import org.planb.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.MethodArgumentNotValidException;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController 
@RequestMapping ("/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<MessageResponse> createUser(@RequestBody @Valid CreateUserRequest request) {

        boolean created = userService.createUser(request);

        if (created) {
            return ResponseEntity.ok(new MessageResponse("User created successfully"));
        } else {
            return ResponseEntity.badRequest().body(new MessageResponse("User already exists"));
        }
    
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(Authentication authentication) {
        return ResponseEntity.ok(userService.getCurrentUser(authentication.getName()));
    }

    @ExceptionHandler (MethodArgumentNotValidException.class)
    public ResponseEntity<MessageResponse> handleValidationError(MethodArgumentNotValidException ex) {
        String errorMessage = ex.getBindingResult().getFieldErrors().get(0).getDefaultMessage();

        return ResponseEntity.badRequest().body(new MessageResponse(errorMessage));
    }

}
