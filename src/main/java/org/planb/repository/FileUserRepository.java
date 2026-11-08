package org.planb.repository;

import org.planb.model.User;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Optional;
import java.util.Properties;




public class FileUserRepository implements UserRepository {

    private final Path filePath = Paths.get("data", "users.properties");

    @Override
    public synchronized User save(User user) {

        Properties properties = loadProperties();

        Long id = user.getId();

        if(id == null) {
            id = nextId(properties);
        }

        String prefix = "user." + id + ".";

        properties.setProperty(prefix + "firstname", user.getFirstname());
        properties.setProperty(prefix + "lastname", user.getLastname());
        properties.setProperty(prefix + "email", user.getEmail());
        properties.setProperty(prefix + "passwordHash", user.getPasswordHash());
        storeProperties(properties);

        return new User(id, user.getFirstname(), user.getLastname(), normalizeEmail(user.getEmail()), user.getPasswordHash());
    }

    @Override
    public synchronized boolean existsByEmail(String email) {

        return findByEmail(email).isPresent();
    }

    private Properties loadProperties() {
        Properties properties = new Properties();
        if (!Files.exists(filePath)) {
            return properties;
        }
        try (InputStream inputStream = Files.newInputStream(filePath)) {
            properties.load(inputStream);
            return properties;
        } catch (IOException e) {
            throw new IllegalStateException("User could not be loaded", e);
        }

    }

    @Override
    public synchronized Optional<User> findByEmail(String email) {

        String normalizedEmail = normalizeEmail(email);
        Properties properties =  loadProperties();

        for (String key : properties.stringPropertyNames()) {
            if (!key.startsWith("user.") || !key.endsWith(".email")) {
                continue;
            }
            String storedEmail = properties.getProperty(key);
            if (!normalizeEmail(storedEmail).equals(normalizedEmail)) {
                continue;
            }
            String idText = key.substring("user.".length(), key.length() - ".email".length());

            try {
                long id = Long.parseLong(idText);
                return Optional.of(readUser(properties, id));
            } catch (NumberFormatException e) {
                throw new IllegalStateException("Invalid user ID format in properties file: " + idText, e);
            }
        }

        return Optional.empty();
    }

    @Override
    public synchronized Optional<User> findById(Long id) {

        if(id == null) {
            return Optional.empty();
        }

        Properties properties = loadProperties();
        String email = properties.getProperty("user." + id + ".email");

        if (email == null) {
            return Optional.empty();
        }

        return Optional.of(readUser(properties, id));
    }

    private User readUser(Properties properties, long id) {
        String prefix = "user." + id + ".";
        String firstname = properties.getProperty(prefix + "firstname");
        String lastname = properties.getProperty(prefix + "lastname");
        String email = properties.getProperty(prefix + "email");
        String passwordHash = properties.getProperty(prefix + "passwordHash");

        return new User(id, firstname, lastname, email, passwordHash);
    }

    private long nextId(Properties properties) {
        long id = 1;

        while (properties.containsKey("user." + id + ".email")) {
            id++;
        }
        return id;
    }


    private void storeProperties(Properties properties) {

        try (OutputStream outputStream = Files.newOutputStream(filePath)) {
            properties.store(outputStream, "Plan B Users");
        } catch (IOException e) {
            throw new IllegalStateException("User could not be saved", e);
        }
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}