package com.skillsphere.controller;

import com.skillsphere.domain.User;
import com.skillsphere.dto.content.ProjectRequest;
import com.skillsphere.dto.content.ProjectResponse;
import com.skillsphere.service.content.ProjectService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * HTTP API for project discovery, owner CRUD, member management, and project-image
 * uploads. Authentication is restored from the Bearer JWT before this controller is
 * reached; the authenticated User is passed explicitly to the service for ownership
 * checks rather than trusting a user id supplied by the browser.
 */
@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @GetMapping
    public Page<ProjectResponse> getProjects(
            @RequestParam(required = false) String search,
            @PageableDefault(size = 10, sort = "id") Pageable pageable
    ) {
        return projectService.getProjects(search, pageable);
    }

    @GetMapping("/mine")
    public Page<ProjectResponse> getMyProjects(
            @AuthenticationPrincipal User currentUser,
            @PageableDefault(size = 10, sort = "id") Pageable pageable
    ) {
        return projectService.getMyProjects(currentUser, pageable);
    }

    @GetMapping("/{projectId}")
    public ProjectResponse getProject(@PathVariable Long projectId) {
        return projectService.getProject(projectId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectResponse createProject(
            @Valid @RequestBody ProjectRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        return projectService.create(request, currentUser);
    }

    @PutMapping("/{projectId}")
    public ProjectResponse updateProject(
            @PathVariable Long projectId,
            @Valid @RequestBody ProjectRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        return projectService.update(projectId, request, currentUser);
    }

    @DeleteMapping("/{projectId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProject(
            @PathVariable Long projectId,
            @AuthenticationPrincipal User currentUser
    ) {
        projectService.delete(projectId, currentUser);
    }

    @PostMapping("/{projectId}/members/{userId}")
    public ProjectResponse addMember(
            @PathVariable Long projectId,
            @PathVariable Long userId,
            @AuthenticationPrincipal User currentUser
    ) {
        return projectService.addMember(projectId, userId, currentUser);
    }

    @DeleteMapping("/{projectId}/members/{userId}")
    public ProjectResponse removeMember(
            @PathVariable Long projectId,
            @PathVariable Long userId,
            @AuthenticationPrincipal User currentUser
    ) {
        return projectService.removeMember(projectId, userId, currentUser);
    }

    @PostMapping(value = "/{projectId}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ProjectResponse uploadProjectImage(
            @PathVariable Long projectId,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal User currentUser
    ) {
        return projectService.uploadImage(projectId, file, currentUser);
    }
}
