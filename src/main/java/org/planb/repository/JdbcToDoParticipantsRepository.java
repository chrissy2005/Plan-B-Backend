package org.planb.repository;

import java.util.List;
import java.util.Optional;
import java.util.Collections;
import org.planb.model.ToDoParticipants;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository 
public class JdbcToDoParticipantsRepository implements ToDoParticipantsRepository{
    
    public static final String COLUMNS = "user_id, todo_id";

    private static final RowMapper<ToDoParticipants> TODO_PARTICIPANTS_MAPPER =
        (rs, rowNum) -> new ToDoParticipants(
            rs.getLong("user_id"),
            rs.getLong("todo_id")
        );
    
    private final JdbcTemplate jdbcTemplate;

    public JdbcToDoParticipantsRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override 
    public ToDoParticipants save(ToDoParticipants participant){
        String sql = """
            INSERT INTO todo_participants
                (user_id, todo_id)
            VALUES (?, ?)
            RETURNING
            ON CONFLICT (user_id, todo_id)
            DO NOTHING
            """ + COLUMNS;

            List<ToDoParticipants> saved = jdbcTemplate.query(
            sql,
            TODO_PARTICIPANTS_MAPPER,
            participant.getUserId(),
            participant.getToDoId()
            );

        if(saved.isEmpty()){
            throw new IllegalStateException("Long-todo participant could not be saved");
        }

        return saved.getFirst();
    }

    @Override 
    public Optional<ToDoParticipants> findByLongToDoAndUserId(Long longToDoId, Long userId){
        if(longToDoId == null || userId ==null){
            return Optional.empty();
        }
        String sql = """
            SELECT %s
            FROM todo_participants
            WHERE user_id = ?
             AND todo_id = ?
            """.formatted(COLUMNS);
        return jdbcTemplate.query(sql, TODO_PARTICIPANTS_MAPPER, userId, longToDoId).stream().findFirst();
    }

    @Override 
    public List<ToDoParticipants> findAllByLongToDoId(Long longToDoId){
        if(longToDoId == null){
            return Collections.emptyList();
        }
        String sql = """
            SELECT %s
            FROM todo_participants
            WHERE todo_id = ?
            ORDER BY user_id
            """.formatted(COLUMNS);
        return jdbcTemplate.query(sql, TODO_PARTICIPANTS_MAPPER, longToDoId);
    }

    @Override 
    public List<ToDoParticipants> findAllByuserId(Long userId){
        if(userId == null){
            return Collections.emptyList();
        }
        String sql = """
            SELECT %s
            FROM todo_participants
            WHERE user_id = ?
            ORDER BY todo_id
            """.formatted(COLUMNS);
        return jdbcTemplate.query(sql, TODO_PARTICIPANTS_MAPPER, userId);
    }

    @Override 
    public boolean existsByLongTodoIdAndUserId(Long longToDoId, Long userId){
        if(longToDoId == null || userId == null){
            return false;
        }
        String sql = """
            SELECT EXISTS(
                SELECT 1
                FROM todo_participants
                WHERE user_id = ?
                 AND todo_id
            ) """;
        Boolean exists = jdbcTemplate.queryForObject(sql, Boolean.class, userId, longToDoId);
        return Boolean.TRUE.equals(exists);
    }

    @Override 
    public void deleteByLongTodoIdAndUserId(Long longToDoId, Long userId){
        if(longToDoId == null || userId == null){
            return;
        }
        String sql = """
            DELETE
            FROM todo_participants
            WHERE user_id = ?
             AND todo_id = ?
            """;
        jdbcTemplate.update(sql, userId, longToDoId);
    }

    @Override 
    public void deleteAllByLongTodoId(Long longToDoId){
        if(longToDoId == null){
            return;
        }
        String sql = """
            DELETE
            FROM todo_participants
            WHERE todo_id = ?
            """;
        jdbcTemplate.update(sql, longToDoId);
    }
}
