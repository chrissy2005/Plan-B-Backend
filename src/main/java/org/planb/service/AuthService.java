package org.planb.service;

import org.planb.model.User;
import org.planb.repository.UserRepository;
import org.planb.dto.LoginRequest;
import org.planb.dto.LoginResponse;
import org.planb.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.Locale;
import java.util.Optional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public Optional<LoginResponse> login(LoginRequest request) {
        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);
        Optional<User> foundUser = userRepository.findByEmail(email);
        if (foundUser.isEmpty()) {
            return Optional.empty();
        }
            User user = foundUser.get();
            boolean passwordCorrect = passwordEncoder.matches(request.getPassword(), user.getPasswordHash());
            if (!passwordCorrect) {
                return Optional.empty();
            }
            String token = jwtService.generateToken(user);

            LoginResponse response = new LoginResponse(token, "Login successful", user.getId());
            return Optional.of(response);
    }
}
