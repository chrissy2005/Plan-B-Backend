package org.planb.service;

import org.planb.repository.EmailAlreadyExistsException;

import org.planb.model.User;
import org.planb.repository.UserRepository;
import org.planb.dto.CreateUserRequest;
import org.planb.dto.UserResponse;
import org.springframework.stereotype.Service;

import java.util.Locale;
import org.springframework.security.crypto.password.PasswordEncoder;





@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }


    public boolean createUser(CreateUserRequest request) {

        String firstname = request.getFirstName().trim();
        String lastname = request.getLastName().trim();
        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmail(email)) {
            return false;
        }

        String passwordHash = passwordEncoder.encode(request.getPassword());

        User user = new User(null, firstname, lastname, email, passwordHash);

        try {
            userRepository.save(user);
            return true;
        } catch (EmailAlreadyExistsException ex) {
            return false;
        }
    }

    public UserResponse getCurrentUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Authenticated user could not be found"));
        return new UserResponse(
                user.getId(),
                user.getFirstname(),
                user.getLastname(),
                user.getEmail()
        );
    }
}
