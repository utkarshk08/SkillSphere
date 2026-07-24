package com.skillsphere.service.content;

import com.skillsphere.domain.Community;
import com.skillsphere.domain.Project;
import com.skillsphere.domain.ReportedContentType;
import com.skillsphere.domain.Role;
import com.skillsphere.domain.User;
import com.skillsphere.dto.content.ProjectRequest;
import com.skillsphere.dto.content.ProjectResponse;
import com.skillsphere.dto.content.UserSummaryResponse;
import com.skillsphere.exception.BadRequestException;
import com.skillsphere.exception.FileUploadException;
import com.skillsphere.exception.ResourceNotFoundException;
import com.skillsphere.exception.UnauthorizedException;
import com.skillsphere.repository.CommunityRepository;
import com.skillsphere.repository.ProjectRepository;
import com.skillsphere.repository.UserRepository;
import com.skillsphere.service.report.ReportService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implements project CRUD and the intentionally small member-management workflow.
 *
 * A project is owned by its creator, who is added as its first member. Only that
 * owner can edit the project or add/remove collaborators. This is a simple,
 * explainable authorization rule that avoids complex project roles while still
 * demonstrating JPA relationships, validation, pagination, and ownership checks.
 */
@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final CommunityRepository communityRepository;
    private final UserRepository userRepository;
    private final ReportService reportService;
    private final Path uploadDirectory;

    public ProjectService(
            ProjectRepository projectRepository,
            CommunityRepository communityRepository,
            UserRepository userRepository,
            ReportService reportService,
            @Value("${app.file.upload-dir}") String uploadDirectory
    ) {
        this.projectRepository = projectRepository;
        this.communityRepository = communityRepository;
        this.userRepository = userRepository;
        this.reportService = reportService;
        this.uploadDirectory = Paths.get(uploadDirectory).toAbsolutePath().normalize();
    }

    @Transactional(readOnly = true)
    public Page<ProjectResponse> getProjects(String search, Pageable pageable) {
        String normalizedSearch = normalizeSearch(search);
        Page<Project> projects = normalizedSearch == null
                ? projectRepository.findAll(pageable)
                : projectRepository.findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
                        normalizedSearch,
                        normalizedSearch,
                        pageable
                );
        return projects.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public ProjectResponse getProject(Long projectId) {
        return toResponse(findProject(projectId));
    }

    @Transactional(readOnly = true)
    public Page<ProjectResponse> getMyProjects(User currentUser, Pageable pageable) {
        User owner = requireCurrentUser(currentUser);
        return projectRepository.findByOwnerId(owner.getId(), pageable).map(this::toResponse);
    }

    @Transactional
    public ProjectResponse create(ProjectRequest request, User currentUser) {
        User owner = requireCurrentUser(currentUser);
        Project project = new Project();
        applyRequest(project, request);
        project.setOwner(owner);
        project.getMembers().add(owner);
        return toResponse(projectRepository.save(project));
    }

    @Transactional
    public ProjectResponse update(Long projectId, ProjectRequest request, User currentUser) {
        Project project = findProject(projectId);
        ensureOwner(project, currentUser);

        if (request.maximumMembers() < project.getCurrentMemberCount()) {
            throw new BadRequestException(
                    "Maximum members cannot be less than the current member count ("
                            + project.getCurrentMemberCount() + ")."
            );
        }

        applyRequest(project, request);
        return toResponse(projectRepository.save(project));
    }

    @Transactional
    public void delete(Long projectId, User currentUser) {
        Project project = findProject(projectId);
        ensureOwner(project, currentUser);
        projectRepository.delete(project);
        reportService.resolveDeletedContent(ReportedContentType.PROJECT, projectId);
    }

    @Transactional
    public ProjectResponse addMember(Long projectId, Long userId, User currentUser) {
        Project project = findProject(projectId);
        ensureOwner(project, currentUser);

        User member = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        if (containsMember(project, member.getId())) {
            throw new BadRequestException("This student is already a project member.");
        }
        if (project.getCurrentMemberCount() >= project.getMaximumMembers()) {
            throw new BadRequestException("This project has no open member positions.");
        }

        project.getMembers().add(member);
        return toResponse(projectRepository.save(project));
    }

    @Transactional
    public ProjectResponse removeMember(Long projectId, Long userId, User currentUser) {
        Project project = findProject(projectId);
        ensureOwner(project, currentUser);

        if (project.getOwner().getId().equals(userId)) {
            throw new BadRequestException("The project owner cannot be removed from the project.");
        }
        if (!containsMember(project, userId)) {
            throw new ResourceNotFoundException("Project member not found: " + userId);
        }

        project.getMembers().removeIf(member -> Objects.equals(member.getId(), userId));
        return toResponse(projectRepository.save(project));
    }

    /**
     * Stores one allowed image in the existing local uploads directory and saves only
     * its relative path. Local storage is deliberately sufficient for this single
     * server project; a cloud object store would be a later deployment concern.
     */
    @Transactional
    public ProjectResponse uploadImage(Long projectId, MultipartFile file, User currentUser) {
        Project project = findProject(projectId);
        ensureOwnerOrAdmin(project, currentUser);

        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Please select a non-empty project image.");
        }

        String extension = extensionFor(file);
        String storedFileName = UUID.randomUUID() + extension;
        Path projectsDirectory = uploadDirectory.resolve("projects").normalize();
        Path destination = projectsDirectory.resolve(storedFileName).normalize();

        try {
            Files.createDirectories(projectsDirectory);
            file.transferTo(destination);
        } catch (IOException | IllegalStateException exception) {
            throw new FileUploadException("Unable to store the project image.", exception);
        }

        project.getProjectImages().add("projects/" + storedFileName);
        return toResponse(projectRepository.save(project));
    }

    private Project findProject(Long projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found: " + projectId));
    }

    private void ensureOwner(Project project, User currentUser) {
        User user = requireCurrentUser(currentUser);
        if (!project.getOwner().getId().equals(user.getId())) {
            throw new UnauthorizedException("Only the project owner can manage this project.");
        }
    }

    private void ensureOwnerOrAdmin(Project project, User currentUser) {
        User user = requireCurrentUser(currentUser);
        boolean isOwner = project.getOwner().getId().equals(user.getId());
        boolean isAdmin = user.getRole() == Role.ROLE_ADMIN;
        if (!isOwner && !isAdmin) {
            throw new UnauthorizedException("Only the project owner or an administrator can upload project images.");
        }
    }

    private User requireCurrentUser(User currentUser) {
        if (currentUser == null || currentUser.getId() == null) {
            throw new UnauthorizedException("Authentication is required for this action.");
        }
        return currentUser;
    }

    private boolean containsMember(Project project, Long userId) {
        return project.getMembers().stream().anyMatch(member -> Objects.equals(member.getId(), userId));
    }

    private void applyRequest(Project project, ProjectRequest request) {
        project.setTitle(request.title().trim());
        project.setDescription(request.description().trim());
        project.setGithubLink(trimToNull(request.githubLink()));
        // Image paths only come from the authenticated multipart upload endpoint.
        // This prevents a request body from injecting arbitrary local or external paths.
        project.setTechStack(normalizeValues(request.techStack()));
        project.setRequiredSkills(normalizeValues(request.requiredSkills()));
        project.setDeadline(request.deadline());
        project.setMaximumMembers(request.maximumMembers());
        project.setStatus(request.status());
        project.setDifficultyLevel(request.difficultyLevel());
        project.setCommunity(resolveCommunity(request.communityId()));
    }

    private Community resolveCommunity(Long communityId) {
        if (communityId == null) {
            return null;
        }
        return communityRepository.findById(communityId)
                .orElseThrow(() -> new ResourceNotFoundException("Community not found: " + communityId));
    }

    private Set<String> normalizeValues(Set<String> values) {
        if (values == null) {
            return new LinkedHashSet<>();
        }
        return values.stream()
                .map(String::trim)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String extensionFor(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null) {
            throw new BadRequestException("Project image type must be JPG, PNG, or WEBP.");
        }

        return switch (contentType.toLowerCase(Locale.ROOT)) {
            case "image/jpeg", "image/jpg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> throw new BadRequestException("Project image type must be JPG, PNG, or WEBP.");
        };
    }

    private ProjectResponse toResponse(Project project) {
        Community community = project.getCommunity();
        Set<UserSummaryResponse> members = project.getMembers().stream()
                .sorted(Comparator.comparing(User::getUsername, String.CASE_INSENSITIVE_ORDER))
                .map(this::toUserSummary)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        return new ProjectResponse(
                project.getId(),
                project.getTitle(),
                project.getDescription(),
                project.getGithubLink(),
                new LinkedHashSet<>(project.getProjectImages()),
                new LinkedHashSet<>(project.getTechStack()),
                new LinkedHashSet<>(project.getRequiredSkills()),
                project.getDeadline(),
                project.getMaximumMembers(),
                project.getStatus(),
                project.getDifficultyLevel(),
                project.getOwner().getId(),
                project.getOwner().getUsername(),
                community == null ? null : community.getId(),
                community == null ? null : community.getName(),
                project.getCurrentMemberCount(),
                project.getOpenPositions(),
                members
        );
    }

    private UserSummaryResponse toUserSummary(User user) {
        return new UserSummaryResponse(
                user.getId(),
                user.getUsername(),
                user.getFullName(),
                user.getProfilePicturePath()
        );
    }

    private String normalizeSearch(String search) {
        return StringUtils.hasText(search) ? search.trim() : null;
    }
}
