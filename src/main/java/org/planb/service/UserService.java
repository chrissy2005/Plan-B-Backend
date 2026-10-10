package org.planb.service;

import org.planb.model.User;
import org.planb.dto.CreateUserRequest;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class UserService {
    private final Map<String, User> users = new ConcurrentHashMap<>();
    private final AtomicLong nextId = new AtomicLong(1);

    public boolean createUser(CreateUserRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        User user = new User(nextId.getAndIncrement(), request.getFirstname().trim(), request.getLastname().trim(), email, request.getPassword());
        return users.putIfAbsent(email, user) == null;
    }
}
