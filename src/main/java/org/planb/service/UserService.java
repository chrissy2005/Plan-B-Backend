package org.planb.service;

import org.planb.model.User;
import org.planb.repository.UserRepository;
import org.planb.dto.CreateUserRequest;
import org.springframework.stereotype.Service;
//import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import java.util.Locale;
import org.springframework.security.crypto.password.PasswordEncoder;

//import java.util.Map;
//import java.util.concurrent.ConcurrentHashMap;
//import java.util.concurrent.atomic.AtomicLong;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /* Version with in-memory storage
    private final Map<String, User> users = new ConcurrentHashMap<>();
    private final AtomicLong nextId = new AtomicLong(1);

    public boolean createUser(CreateUserRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        User user = new User(nextId.getAndIncrement(), request.getFirstname().trim(), request.getLastname().trim(), email, request.getPassword());
        return users.putIfAbsent(email, user) == null;
    }
    */
    public boolean createUser(CreateUserRequest request) {

        String firstname = request.getFirstname().trim();
        String lastname = request.getLastname().trim();
        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmail(email)) {
            return false; // User already exists
        }

        String passwordHash = passwordEncoder.encode(request.getPassword());

        User user = new User(null, firstname, lastname, email, passwordHash);
        userRepository.save(user);
        return true; // User created successfully
    }
}
