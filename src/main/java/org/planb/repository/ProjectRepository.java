package org.planb.repository;

import org.planb.model.Project;
//import org.planb.model.ProjectStatus;
import java.util.List;
import java.util.Optional;

public interface ProjectRepository {

  Project save(Project project);    //for updating and saving the Project(-data)
  
  Optional<Project> findByProjectId(Long projectId);   

  List<Project> findByCreatedUserId(Long createdUserId);

  List<Project> findByProjectIds(List<Long> projectIds);

  boolean existsByProjectId(Long projectId);

  void deleteByProjectId(Long projectId);
} 