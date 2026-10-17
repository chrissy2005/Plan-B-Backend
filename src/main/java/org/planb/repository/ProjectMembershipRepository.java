package org.planb.repository;

import org.planb.model.ProjectMembership;
import org.planb.model.ProjectRole;
import java.util.List;
import java.util.Optional;

public interface ProjectMembershipRepository {
    
    ProjectMembership save(ProjectMembership membership);   //sava and update ProjectMembership

    Optional<ProjectMembership> findByProjectIdAndUserId(Long projectId, Long userId);  //find Role of one projectmember

    List<ProjectMembership> findAllByProjectId(Long projectId);    //find all Projectmembers of one Project

    List<ProjectMembership> findAllByUserId(Long userId);          //find all Projects of one user

    List<ProjectMembership> findByProjectIdAndRole(Long projectId, ProjectRole role);   //find all Project where user has this role

    boolean existsByProjectIdAndUserId(Long projectId, Long userId);    //check if user is already a projectmember

    void deleteByProjectIdAndUSerId(Long projectId, Long userId);   //delete member from project

    void deleteByProjectId(Long projectId);    //delete all relations from one project 
}
