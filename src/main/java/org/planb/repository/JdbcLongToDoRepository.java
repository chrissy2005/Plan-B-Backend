package org.planb.repository;

import org.planb.model.LongToDo;
import org.planb.model.LongToDoSortField;
import org.planb.model.SortDirection;
import org.planb.model.TaskPriority;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;


import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Repository 
public class JdbcLongToDoRepository implements LongToDoRepository{
        public static final String COLUMNS = "todo_id, project_id, title, description, starts_at, ends_at, priority, is_completed";

    private static final RowMapper<LongToDo> LONG_TODO_MAPPER =
        (rs, rowNum) -> new LongToDo(
            rs.getLong("todo_id"),
            rs.getLong("project_id"),
            rs.getString("title"),
            rs.getString("description"),
            rs.getTimestamp("starts_at").toLocalDateTime(),
            rs.getTimestamp("ends_at").toLocalDateTime(),
            TaskPriority.valueOf(rs.getString("priority")),
            rs.getBoolean("is_completed")
        );
    

    private final JdbcTemplate jdbcTemplate;

    public JdbcLongToDoRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override 
    public LongToDo save(LongToDo longToDo){
        if(longToDo.getToDoId() != null){
            return updateExistingLongToDo(longToDo);
        } 
        
        String sql = """
            INSERT INTO long_todos
                (project_id, title, description, starts_at, ends_at, priority, is_completed)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            RETURNING
            """ + COLUMNS;
        
        List<LongToDo> saved = jdbcTemplate.query(
            sql,
            LONG_TODO_MAPPER,
            longToDo.getProjectId(),
            longToDo.getTitle(),
            longToDo.getDescription(),
            Timestamp.valueOf(longToDo.getStartTime()),
            Timestamp.valueOf(longToDo.getEndTime()),
            longToDo.getPriority().name(),
            longToDo.isDone()
        );

        if(saved.isEmpty()){
            throw new IllegalStateException("Long todo could not be saved");
        }

        return saved.getFirst();
    }    
        
    private LongToDo updateExistingLongToDo(LongToDo longToDo){
        String sql = """
            UPDATE long_todos
            SET project_id = ?,
            title = ?,
            description = ?,
            starts_at = ?,
            ends_at = ?,
            priority = ?,
            is_completed = ?
            WHERE todo_id = ?
            RETURNING
            """ + COLUMNS;
        
        List<LongToDo> updated = jdbcTemplate.query(
            sql,
            LONG_TODO_MAPPER,
            longToDo.getProjectId(),
            longToDo.getTitle(),
            longToDo.getDescription(),
            Timestamp.valueOf(longToDo.getStartTime()),
            Timestamp.valueOf(longToDo.getEndTime()),
            longToDo.getPriority().name(),
            longToDo.isDone(),
            longToDo.getToDoId()
        );

        if(updated.isEmpty()){
           throw new IllegalStateException("Long todo to update does not exist");
        }

        return updated.getFirst();
    }

    @Override 
    public Optional<LongToDo> findById(Long toDoId){
        if(toDoId == null){
            return Optional.empty();
        }
        String sql = """
            SELECT %s
            FROM long_todos
            WHERE todo_id = ?
            """.formatted(COLUMNS);
        return jdbcTemplate.query(sql, LONG_TODO_MAPPER, toDoId).stream().findFirst();
    }

    @Override 
    public List<LongToDo> findAllByProjectId(Long projectId){
        if(projectId == null){
            return Collections.emptyList();
        }
        String sql = """
            SELECT %s
            FROM long_todos
            WHERE todo_id = ?
            ORDER BY todo_id
            """.formatted(COLUMNS);
        return jdbcTemplate.query(sql, LONG_TODO_MAPPER, projectId);
    }

    @Override 
    public List<LongToDo> searchByProject(Long projectId, TaskPriority priority, Boolean done, LocalDateTime from, LocalDateTime to, LongToDoSortField sortField, SortDirection sortDirection){
        if(projectId == null){
            return Collections.emptyList();
        }

        StringBuilder sql = new StringBuilder("""
            SELECT %s
            FROM long_todos
            WHERE project_id = ?
            """.formatted(COLUMNS));
        
            List<Object> parameters = new ArrayList<>();
            parameters.add(projectId);

            if(priority != null){
                sql.append(" AND priority = ? ");
                parameters.add(priority.name());
            }
            if(done != null){
                sql.append(" AND is_completed = ? ");
                parameters.add(done);
            }
            if(from != null){
                sql.append(" AND starts_at >= ? ");
                parameters.add(Timestamp.valueOf(from));
            }
            if(to != null){
                sql.append(" AND ends_at <= ? ");
                parameters.add(Timestamp.valueOf(to));
            }

            sql.append(" ORDER BY");
            sql.append(orderBy(sortField));
            sql.append(" ");
            sql.append(direction(sortDirection));
            sql.append(", todo_id");
            
            return jdbcTemplate.query(sql.toString(), LONG_TODO_MAPPER, parameters.toArray());
    }

    @Override
    public boolean existsById(Long toDoId){
        if(toDoId == null){
            return false;
        }
        String sql = """
            SELECT EXISTS(
                SELECT 1
                FROM long_todos
                WHERE todo_id = ?
            ) """;
        Boolean exists = jdbcTemplate.queryForObject(sql, Boolean.class, toDoId);
        return Boolean.TRUE.equals(exists);
    }

    @Override 
    public void deleteById(Long toDoId){
        if(toDoId == null){
            return;
        }
        String sql = """
            DELETE
            FROM long_todos
            WHERE todo_id = ?
            """;
        jdbcTemplate.update(sql, toDoId);
    }

    private String orderBy(LongToDoSortField sortField){
        if(sortField == null){
            return "todo_id";
        }
        return switch(sortField){
            case START_DATE_TIME -> "starts_at";
            case END_DATE_TIME -> "ends_at";
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
