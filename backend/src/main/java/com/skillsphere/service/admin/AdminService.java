package com.skillsphere.service.admin;

import com.skillsphere.domain.ReportedContentType;
import com.skillsphere.domain.User;
import com.skillsphere.dto.admin.AdminUserResponse;
import com.skillsphere.dto.profile.ProfileResponse;
import com.skillsphere.exception.ResourceNotFoundException;
import com.skillsphere.repository.ProjectRepository;
import com.skillsphere.repository.SkillRepository;
import com.skillsphere.repository.UserRepository;
import com.skillsphere.service.content.CommunityService;
import com.skillsphere.service.profile.ProfileService;
import com.skillsphere.service.report.ReportService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Small administration service for the explicitly requested user management and profile
 * verification actions. Content/report/announcement details remain in their own services.
 */
@Service
public class AdminService {

    private final UserRepository userRepository;
    private final ProfileService profileService;
    private final ProjectRepository projectRepository;
    private final CommunityService communityService;
    private final SkillRepository skillRepository;
    private final ReportService reportService;

    public AdminService(
            UserRepository userRepository,
            ProfileService profileService,
            ProjectRepository projectRepository,
            CommunityService communityService,
            SkillRepository skillRepository,
            ReportService reportService
    ) {
        this.userRepository = userRepository;
        this.profileService = profileService;
        this.projectRepository = projectRepository;
        this.communityService = communityService;
        this.skillRepository = skillRepository;
        this.reportService = reportService;
    }

    @Transactional(readOnly = true)
    public Page<AdminUserResponse> getUsers(String search, Pageable pageable) {
        Page<User> users = search == null || search.isBlank()
                ? userRepository.findAll(pageable)
                : userRepository.findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCaseOrCollegeNameContainingIgnoreCase(
                        search.trim(), search.trim(), search.trim(), pageable);
        return users.map(this::toResponse);
    }

    @Transactional
    public ProfileResponse verifyProfile(Long userId) {
        return profileService.verifyProfile(userId);
    }

    @Transactional
    public void deleteUser(Long userId, User currentAdmin) {
        profileService.deleteByAdmin(userId, currentAdmin);
    }

    /** Deletes a reported project, community, skill, or profile through an explicit admin action. */
    @Transactional
    public void deleteContent(ReportedContentType contentType, Long contentId, User currentAdmin) {
        switch (contentType) {
            case PROFILE -> deleteUser(contentId, currentAdmin);
            case PROJECT -> projectRepository.delete(projectRepository.findById(contentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Project not found: " + contentId)));
            case COMMUNITY -> communityService.delete(contentId);
            case SKILL -> skillRepository.delete(skillRepository.findById(contentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Skill not found: " + contentId)));
        }
        reportService.resolveDeletedContent(contentType, contentId);
    }

    private AdminUserResponse toResponse(User user) {
        return new AdminUserResponse(
                user.getId(), user.getUsername(), user.getEmail(), user.getFullName(), user.getCollegeName(),
                user.getRole(), user.getAuthProvider(), user.isVerified(), user.isPublicProfileVisibility()
        );
    }
}
