package org.planb.repository;

import org.planb.model.User;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Repository
public class JdbcUserRepository implements UserRepository {

    private static final String COLUMNS =
            "user_id, first_name, last_name, email, password_hash";

    private static final RowMapper<User> USER_MAPPER = (rs, rowNum) ->
            new User(
                    rs.getLong("user_id"),
                    rs.getString("first_name"),
                    rs.getString("last_name"),
                    rs.getString("email"),
                    rs.getString("password_hash")
            );

    private final JdbcTemplate jdbcTemplate;

    public JdbcUserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<User> findByEmail(String email) {
        String sql = "SELECT " + COLUMNS
                + " FROM users WHERE email = ?";

        return jdbcTemplate.query(sql, USER_MAPPER, normalizeEmail(email))
                .stream()
                .findFirst();
    }

    @Override
    public Optional<User> findById(Long id) {
        if (id == null) {
            return Optional.empty();
        }

        String sql = "SELECT " + COLUMNS
                + " FROM users WHERE user_id = ?";

        return jdbcTemplate.query(sql, USER_MAPPER, id)
                .stream()
                .findFirst();
    }

    @Override
    public boolean existsByEmail(String email) {
        Boolean exists = jdbcTemplate.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM users WHERE email = ?)",
                Boolean.class,
                normalizeEmail(email)
        );

        return Boolean.TRUE.equals(exists);
    }

    @Override
    public User save(User user) {
        if (user.getId() != null) {
            return updateExistingUser(user);
        }

        String sql = """
                INSERT INTO users (first_name, last_name, email, password_hash)
                VALUES (?, ?, ?, ?)
                ON CONFLICT (email) DO NOTHING
                RETURNING
                """ + COLUMNS;

        List<User> savedUsers = jdbcTemplate.query(
                sql,
                USER_MAPPER,
                user.getFirstname(),
                user.getLastname(),
                normalizeEmail(user.getEmail()),
                user.getPasswordHash()
        );

        if (savedUsers.isEmpty()) {
            throw new EmailAlreadyExistsException();
        }

        return savedUsers.getFirst();
    }

    private User updateExistingUser(User user) {
        String sql = """
                UPDATE users
                SET first_name = ?, last_name = ?, email = ?, password_hash = ?
                WHERE user_id = ?
                RETURNING
                """ + COLUMNS;

        List<User> updatedUsers = jdbcTemplate.query(
                sql,
                USER_MAPPER,
                user.getFirstname(),
                user.getLastname(),
                normalizeEmail(user.getEmail()),
                user.getPasswordHash(),
                user.getId()
        );

        if (updatedUsers.isEmpty()) {
            throw new IllegalStateException("User to update does not exist");
        }

        return updatedUsers.getFirst();
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
