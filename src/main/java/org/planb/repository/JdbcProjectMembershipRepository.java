package org.planb.repository;

import org.planb.model.ProjectMembership;
import org.planb.model.ProjectRole;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository 
public class JdbcProjectMembershipRepository implements ProjectMembershipRepository{

    public static final String COLUMNS = "project_id, user_id, role";

    private static final RowMapper<ProjectMembership> MEMBERSHIP_MAPPER =
        (rs, rowNum) -> new ProjectMembership(
            rs.getLong("project_id"),
            rs.getLong("user_id"),
            ProjectRole.valueOf(rs.getString("role"))
        );
    
    private final JdbcTemplate jdbcTemplate;

    public JdbcProjectMembershipRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }


    @Override 
    public ProjectMembership save(ProjectMembership membership){
        
        String sql = """
            INSERT INTO projectmembership
                (project_id, user_id, role)
            VALUES (?, ?, ?)
            ON CONFLICT (project_id, user_id)
            DO UPDATE SET role = EXCLUDED.role
            RETURNING
                project_id, user_id, role
            """;

        List<ProjectMembership> savedMemberships =
                jdbcTemplate.query(
                    sql,
                    MEMBERSHIP_MAPPER,
                    membership.getProjectId(),
                    membership.getUserId(),
                    membership.getRole().name()
                );

        if (savedMemberships.isEmpty()) {
            throw new IllegalStateException(
                "Project membership could not be saved"
            );
        }

        return savedMemberships.getFirst();
    }

    @Override
    public boolean addIfAbsent(ProjectMembership membership) {
        return jdbcTemplate.update("""
            INSERT INTO projectmembership (project_id, user_id, role)
            VALUES (?, ?, ?)
            ON CONFLICT (project_id, user_id) DO NOTHING
            """, membership.getProjectId(), membership.getUserId(), membership.getRole().name()) == 1;
    }

    @Override 
    public Optional<ProjectMembership> findByProjectIdAndUserId(Long projectId, Long userId){
    
        String sql = """
            SELECT %s
            FROM projectmembership
            WHERE project_id = ?
            AND user_id = ?
            """.formatted(COLUMNS);

        return jdbcTemplate
            .query(
                sql,
                MEMBERSHIP_MAPPER,
                projectId,
                userId
            )
            .stream()
            .findFirst();
    }

    @Override 
    public List<ProjectMembership> findAllByProjectId(Long projectId){
         String sql = """
                SELECT %s
                FROM projectmembership
                WHERE project_id = ?
                ORDER BY user_id
                """.formatted(COLUMNS);

        return jdbcTemplate.query(
                sql,
                MEMBERSHIP_MAPPER,
                projectId
        );
    }

    @Override 
    public List<ProjectMembership> findAllByUserId(Long userId){
        
        String sql = """
            SELECT %s
            FROM projectmembership
            WHERE user_id = ?
            ORDER BY project_id
            """.formatted(COLUMNS);

        return jdbcTemplate.query(
            sql,
            MEMBERSHIP_MAPPER,
            userId
        );    
    }

    @Override 
    public List<ProjectMembership> findByProjectIdAndRole(Long projectId, ProjectRole role){
         String sql = """
            SELECT %s
            FROM projectmembership
            WHERE project_id = ?
            AND role = ?
            """.formatted(COLUMNS);

        return jdbcTemplate.query(
            sql,
            MEMBERSHIP_MAPPER,
            projectId,
            role.name()
        );
    }

    @Override 
    public boolean existsByProjectIdAndUserId(Long projectId, Long userId){
        Boolean exists = jdbcTemplate.queryForObject(
            """
            SELECT EXISTS (
                SELECT 1
                FROM projectmembership
                WHERE project_id = ?
                AND user_id = ?
            )
            """,
            Boolean.class,
            projectId,
            userId
        );

        return Boolean.TRUE.equals(exists);    
    }

    @Override 
    public void deleteByProjectIdAndUSerId(Long projectId, Long userId){
        jdbcTemplate.update(
            """
            DELETE FROM projectmembership
            WHERE project_id = ?
            AND user_id = ?
            """,
            projectId,
            userId
        );
    }

    @Override 
    public void deleteByProjectId(Long projectId){
        jdbcTemplate.update(
            """
            DELETE FROM projectmembership
            WHERE project_id = ?
            """,
            projectId
        );
    }    
}
