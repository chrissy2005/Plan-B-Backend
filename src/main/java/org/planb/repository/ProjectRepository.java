package org.planb.repository;

import org.planb.model.Project;

import java.util.List;
import java.util.Optional;

public interface ProjectRepository {

  Project save(Project project);    
  
  Optional<Project> findByProjectId(Long projectId);   

  List<Project> findByCreatedUserId(Long createdUserId);

  List<Project> findByProjectIds(List<Long> projectIds);

  boolean existsByProjectId(Long projectId);

  void deleteByProjectId(Long projectId);
} 