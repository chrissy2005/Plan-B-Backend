package org.planb.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.planb.model.SortDirection;
import org.planb.model.Subtask;
import org.planb.model.SubtaskSortField;
import org.planb.model.TaskPriority;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.Collections;

@Repository 
public class JdbcSubtaskRepository implements SubtaskRepository{
    public static final String COLUMNS = "title, description, subtask_id, todo_id, is_completed, assignee_id, priority ";

    private static final RowMapper<Subtask> SUBTASK_MAPPER =
        (rs, rowNum) -> new Subtask(
            rs.getString("title"),
            rs.getString("description"),
            rs.getLong("subtask_id"),
            rs.getLong("todo_id"),
            rs.getBoolean("is_completed"),
            rs.getObject("assignee_id", Long.class), //KI Log für Werte die null sein dürfen
            TaskPriority.valueOf(rs.getString("priority"))
        );

    private final JdbcTemplate jdbcTemplate;

    public JdbcSubtaskRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override 
    public Subtask save(Subtask subtask){
        if(subtask.getSubtaskId() != null){
            return updateExistingSubtask(subtask);
        }

        String sql = """
            INSERT INTO subtasks
                ( title, description, todo_id, is_completed, assignee_id, priority )
            VALUES (?, ?, ?, ?, ?, ?)
            RETURNING
            """ + COLUMNS;
        
        List<Subtask> saved = jdbcTemplate.query(
            sql,
            SUBTASK_MAPPER,
            subtask.getTitle(),
            subtask.getDescription(),
            subtask.getLongToDoId(),
            subtask.isDone(),
            subtask.getAssigneeId(),
            subtask.getPriority().name()
        );

        if(saved.isEmpty()){
            throw new IllegalStateException("Subtask could not be saved");
        }

        return saved.getFirst();
    }

    private Subtask updateExistingSubtask(Subtask subtask){
        String sql = """
            UPDATE subtasks
            SET title = ?,
            description = ?,
            todo_id = ?,
            is_completed = ?,
            assignee_id = ?,
            priority = ? 
            WHERE subtask_id = ?
            RETURNING
            """ + COLUMNS;
        
        List<Subtask> updated = jdbcTemplate.query(
            sql,
            SUBTASK_MAPPER,
            subtask.getTitle(),
            subtask.getDescription(),
            subtask.getLongToDoId(),
            subtask.isDone(),
            subtask.getAssigneeId(),
            subtask.getPriority().name(),
            subtask.getSubtaskId()
        );

        if(updated.isEmpty()){
            throw new IllegalStateException("Subtask to update does not exists");
        }

        return updated.getFirst();
    }

    @Override 
    public Optional<Subtask> findById(Long subtaskId){
        if(subtaskId == null){
            return Optional.empty();
        }
        String sql = """
            SELECT %s
            FROM subtasks
            WHERE subtask_id = ?
            """.formatted(COLUMNS);
        return jdbcTemplate.query(sql, SUBTASK_MAPPER, subtaskId).stream().findFirst();
    }

    @Override 
    public Optional<Subtask> findByIdAndLongToDoId(Long subtaskId, Long longToDoId){
        if(subtaskId == null || longToDoId == null){
            return Optional.empty();
        }
        String sql = """
            SELECT %s
            FROM subtasks
            WHERE subtask_id = ? 
             AND todo_id = ?
            """.formatted(COLUMNS);
        return jdbcTemplate.query(sql, SUBTASK_MAPPER, subtaskId, longToDoId).stream().findFirst();
    }

    @Override 
    public List<Subtask> findByLongToDoId(Long longToDoId){
        if(longToDoId == null){
            return Collections.emptyList();
        }
        String sql = """
            SELECT %s
            FROM subtasks
            WHERE todo_id = ?
            ORDER BY subtask_id
            """.formatted(COLUMNS);
        return jdbcTemplate.query(sql, SUBTASK_MAPPER, longToDoId);
    }
    
    @Override 
    public List<Subtask> searchByLongTodo(Long longToDoId, TaskPriority priority, Boolean done, Long assigneeId, SubtaskSortField sortField, SortDirection sortDirection){
        if(longToDoId == null){
            return Collections.emptyList();
        }

        StringBuilder sql = new StringBuilder("""
            SELECT %s
            FROM subtasks
            WHERE todo_id = ?
            """.formatted(COLUMNS));
        
            List<Object> parameters = new ArrayList<>();
            parameters.add(longToDoId);

            if(priority != null){
                sql.append(" AND priority = ? ");
                parameters.add(priority.name());
            }
            if(done != null){
                sql.append(" AND is_completed = ? ");
                parameters.add(done);
            }
            if(assigneeId != null){
                sql.append(" AND assignee_id = ? ");
                parameters.add(assigneeId);   
            }


            sql.append(" ORDER BY");
            sql.append(orderBy(sortField));
            sql.append(" ");
            sql.append(direction(sortDirection));
            sql.append(", todo_id");
            
            return jdbcTemplate.query(sql.toString(), SUBTASK_MAPPER, parameters.toArray());
    }

    @Override 
    public Boolean existsById(Long subtaskId){
        if(subtaskId == null){
            return false;
        }
        String sql = """
            SELECT EXISTS(
                SELECT 1
                FROM subtasks
                WHERE subtask_id = ?
            ) """;
        Boolean exists = jdbcTemplate.queryForObject(sql, Boolean.class, subtaskId);
        return Boolean.TRUE.equals(exists);
    }

    @Override
    public int claimIfUnassigned(Long subtaskId, Long userId) {
        return jdbcTemplate.update("""
            UPDATE subtasks
            SET assignee_id = ?
            WHERE subtask_id = ?
              AND assignee_id IS NULL
            """, userId, subtaskId);
    }

    @Override
    public int releaseIfAssignedTo(Long subtaskId, Long userId) {
        return jdbcTemplate.update("""
            UPDATE subtasks
            SET assignee_id = NULL
            WHERE subtask_id = ?
              AND assignee_id = ?
            """, subtaskId, userId);
    }

    @Override 
    public void deleteById(Long subtaskId){
        if(subtaskId == null){
            return;
        }
        String sql = """
            DELETE
            FROM subtasks
            WHERE subtask_id = ?
            """;
        jdbcTemplate.update(sql, subtaskId);
    }

    @Override 
    public void deleteAllByLongToDoId(Long longToDoId){
        if(longToDoId == null){
            return;
        }
        String sql = """
            DELETE
            FROM subtasks
            WHERE todo_id = ?
            """;
        jdbcTemplate.update(sql, longToDoId);
    }

    private String orderBy(SubtaskSortField sortField){
        if(sortField == null){
            return "subtask_id";
        }
        return switch(sortField){
            case DONE -> "is_completed";
            case PRIORITY -> """
                CASE priority
                    CASE WHEN 'HIGH' THEN 1
                    CASE WHEN 'MEDIUM' THEN 2
                    CASE WHEN 'LOW' THEN 3
                    ELSE 4
                END
                """;
        };
    }

    private String direction(SortDirection sortDirection){
        return sortDirection == SortDirection.DESC
                ? "DESC"
                : "ASC";
    }
}
