package org.planb.repository;

import org.planb.model.Project;
import org.planb.model.ProjectStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.Collections; // KI Log Nr. 26
import java.util.List;
import java.util.Objects; // KI Log Nr. 33
import java.util.Optional;


@Repository
public class JdbcProjectRepository implements ProjectRepository {
    
private static final String COLUMNS =
"project_id, name, description, status, created_user_id";

    private static final RowMapper<Project> PROJECT_MAPPER = (rs, rowNum) ->
            new Project(
                    rs.getLong("project_id"),
                    rs.getString("name"),
                    rs.getString("description"),
                    rs.getObject("created_user_id", Long.class), // KI Log Nr. 28
                    ProjectStatus.valueOf(rs.getString("status"))
                    
            );

    private final JdbcTemplate jdbcTemplate;

    public JdbcProjectRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Project save(Project project) {
        if (project.getId() != null) {
            return updateExistingProject(project);
    }

    //KI Log Nr. 27
     String sql = """
                INSERT INTO projects (name, description, status, created_user_id)
                VALUES (?, ?, ?, ?)
                RETURNING
                """ + COLUMNS;

    List<Project> savedProjects = jdbcTemplate.query(
                sql,
                PROJECT_MAPPER,
                project.getName(),
                project.getDescription(),
                statusOrDefault(project.getProjectStatus()).name(),
                project.getCreatedByUserId()
        );

        return savedProjects.getFirst();
        }

private Project updateExistingProject(Project project) {
        //KI Log Nr. 30
        String sql = """
                UPDATE projects
                SET name = ?, description = ?, status = ?
                WHERE project_id = ?
                RETURNING
                """ + COLUMNS;

        List<Project> updatedProjects = jdbcTemplate.query(
                sql,
                PROJECT_MAPPER,
                project.getName(),
                project.getDescription(),
                statusOrDefault(project.getProjectStatus()).name(),
                project.getId()
        );

        if (updatedProjects.isEmpty()) {
            throw new IllegalStateException("Project to update does not exist");
        }

        return updatedProjects.getFirst();
    }
   

@Override
    public Optional<Project> findByProjectId(Long projectId) {
        if (projectId == null) {
            return Optional.empty();
        }

        //KI Log Nr. 31
        String sql = "SELECT " + COLUMNS
                + " FROM projects WHERE project_id = ?";
        //KI Log Nr. 31
        return jdbcTemplate.query(sql, PROJECT_MAPPER, projectId)
                .stream()
                .findFirst();
    }

@Override
    public List<Project> findByCreatedUserId(Long createdUserId){
        if (createdUserId == null) {
            return Collections.emptyList(); //KI Log Nr. 26
        }

        //KI Log Nr. 26
        String sql = "SELECT " + COLUMNS
                + " FROM projects WHERE created_user_id = ? ORDER BY project_id";
 
        return jdbcTemplate.query(sql, PROJECT_MAPPER, createdUserId);
    }

@Override
    public List<Project> findByProjectIds(List<Long> projectIds){
        if (projectIds == null) {
            return Collections.emptyList(); //KI Log Nr. 26
        }

        //KI Log Nr. 33
        // null-Einträge und Duplikate entfernen
        List<Long> ids = projectIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        
        //KI Log Nr. 33
        // Leere Liste abfangen: "IN ()" wäre ungültiges SQL
        if (ids.isEmpty()) {
            return Collections.emptyList();
        }
        //KI Log Nr. 33
        // Ein Platzhalter pro ID -> "IN (?, ?, ?)"; Werte nie direkt ins SQL schreiben (SQL-Injection)
        String placeholders = String.join(", ", Collections.nCopies(ids.size(), "?"));
        String sql = "SELECT " + COLUMNS
                + " FROM projects WHERE project_id IN (" + placeholders + ") ORDER BY project_id";
 
        return jdbcTemplate.query(sql, PROJECT_MAPPER, ids.toArray());
    }

@Override
    public boolean existsByProjectId(Long projectId){
        if (projectId == null) {
            return false;
        }

        Boolean exists = jdbcTemplate.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM projects WHERE project_id = ?)",
                Boolean.class,
                projectId
        );

        return Boolean.TRUE.equals(exists);
    }

@Override
    public void deleteByProjectId(Long projectId){
        if (projectId == null) {
            return;
        }

        //KI Log Nr. 32
        jdbcTemplate.update("DELETE FROM projects WHERE project_id = ?", projectId);
    }
  //KI Log Nr. 32
    private ProjectStatus statusOrDefault(ProjectStatus status) {
        return status != null ? status : ProjectStatus.ACTIVE;
    }
    }