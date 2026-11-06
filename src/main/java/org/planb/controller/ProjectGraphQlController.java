package org.planb.controller;

import java.security.Principal;
import java.util.List;

import org.planb.dto.ProjectsViewResponse;
import org.planb.service.ProjectService;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.stereotype.Controller;

@Controller
public class ProjectGraphQlController {

    private final ProjectService projectService;

    public ProjectGraphQlController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @QueryMapping
    public List<ProjectsViewResponse> myProjects(Principal principal) {
        if (principal == null) {
            throw new AuthenticationCredentialsNotFoundException(
                "Authentication is required"
            );
        }

        return projectService.getProjectView(principal.getName());
    }
}