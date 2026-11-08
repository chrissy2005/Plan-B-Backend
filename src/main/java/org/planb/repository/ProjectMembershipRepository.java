package org.planb.repository;

import org.planb.model.ProjectMembership;
import org.planb.model.ProjectRole;
import java.util.List;
import java.util.Optional;

public interface ProjectMembershipRepository {

    ProjectMembership save(ProjectMembership membership);
    boolean addIfAbsent(ProjectMembership membership);

    Optional<ProjectMembership> findByProjectIdAndUserId(Long projectId, Long userId);

    List<ProjectMembership> findAllByProjectId(Long projectId);

    List<ProjectMembership> findAllByUserId(Long userId);

    List<ProjectMembership> findByProjectIdAndRole(Long projectId, ProjectRole role);

    boolean existsByProjectIdAndUserId(Long projectId, Long userId);

    void deleteByProjectIdAndUSerId(Long projectId, Long userId);

    void deleteByProjectId(Long projectId);
}
