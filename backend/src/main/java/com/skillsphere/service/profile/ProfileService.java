package com.skillsphere.service.profile;

import com.skillsphere.domain.NotificationType;
import com.skillsphere.domain.Community;
import com.skillsphere.domain.Project;
import com.skillsphere.domain.ReportedContentType;
import com.skillsphere.domain.Role;
import com.skillsphere.domain.Skill;
import com.skillsphere.domain.SkillIntent;
import com.skillsphere.domain.User;
import com.skillsphere.dto.profile.ProfileResponse;
import com.skillsphere.dto.profile.ProfileUpdateRequest;
import com.skillsphere.exception.BadRequestException;
import com.skillsphere.exception.FileUploadException;
import com.skillsphere.exception.ResourceNotFoundException;
import com.skillsphere.exception.UnauthorizedException;
import com.skillsphere.repository.ProjectRepository;
import com.skillsphere.repository.AnnouncementRepository;
import com.skillsphere.repository.BookmarkRepository;
import com.skillsphere.repository.CollaborationRequestRepository;
import com.skillsphere.repository.CommunityRepository;
import com.skillsphere.repository.NotificationRepository;
import com.skillsphere.repository.ReportRepository;
import com.skillsphere.repository.RoadmapRepository;
import com.skillsphere.repository.SkillRepository;
import com.skillsphere.repository.UserRepository;
import com.skillsphere.service.notification.NotificationService;
import com.skillsphere.service.report.ReportService;
import com.skillsphere.service.storage.ImageStorageService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Handles the profile portion of User without a separate Profile entity. One student
 * has one profile, so keeping its fields on the account is simpler and still supports
 * the requested profile CRUD, visibility, verification, search, and photo upload.
 */
@Service
public class ProfileService {

    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

    private final UserRepository userRepository;
    private final SkillRepository skillRepository;
    private final ProjectRepository projectRepository;
    private final CommunityRepository communityRepository;
    private final RoadmapRepository roadmapRepository;
    private final BookmarkRepository bookmarkRepository;
    private final CollaborationRequestRepository collaborationRequestRepository;
    private final NotificationRepository notificationRepository;
    private final ReportRepository reportRepository;
    private final AnnouncementRepository announcementRepository;
    private final NotificationService notificationService;
    private final ReportService reportService;
    private final ImageStorageService imageStorageService;

    public ProfileService(
            UserRepository userRepository,
            SkillRepository skillRepository,
            ProjectRepository projectRepository,
            CommunityRepository communityRepository,
            RoadmapRepository roadmapRepository,
            BookmarkRepository bookmarkRepository,
            CollaborationRequestRepository collaborationRequestRepository,
            NotificationRepository notificationRepository,
            ReportRepository reportRepository,
            AnnouncementRepository announcementRepository,
            NotificationService notificationService,
            ReportService reportService,
            ImageStorageService imageStorageService
    ) {
        this.userRepository = userRepository;
        this.skillRepository = skillRepository;
        this.projectRepository = projectRepository;
        this.communityRepository = communityRepository;
        this.roadmapRepository = roadmapRepository;
        this.bookmarkRepository = bookmarkRepository;
        this.collaborationRequestRepository = collaborationRequestRepository;
        this.notificationRepository = notificationRepository;
        this.reportRepository = reportRepository;
        this.announcementRepository = announcementRepository;
        this.notificationService = notificationService;
        this.reportService = reportService;
        this.imageStorageService = imageStorageService;
    }

    @Transactional(readOnly = true)
    public Page<ProfileResponse> search(
            String name,
            String college,
            String country,
            String skill,
            String interest,
            Pageable pageable
    ) {
        return userRepository.searchPublicProfiles(
                normalize(name), normalize(college), normalize(country), normalize(skill), normalize(interest),
                Role.ROLE_USER, pageable
        ).map(user -> toResponse(user, false));
    }

    @Transactional(readOnly = true)
    public ProfileResponse getCurrent(User currentUser) {
        return toResponse(findUser(currentUser.getId()), true);
    }

    @Transactional(readOnly = true)
    public ProfileResponse getByUsername(String username, User viewer) {
        User profile = userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found: " + username));
        boolean canView = profile.isPublicProfileVisibility()
                || (viewer != null && (viewer.getId().equals(profile.getId()) || viewer.getRole() == Role.ROLE_ADMIN));
        if (!canView) {
            throw new UnauthorizedException("This profile is private.");
        }
        boolean canViewPrivateFields = viewer != null
                && (viewer.getId().equals(profile.getId()) || viewer.getRole() == Role.ROLE_ADMIN);
        return toResponse(profile, canViewPrivateFields);
    }

    @Transactional
    public ProfileResponse update(ProfileUpdateRequest request, User currentUser) {
        User user = findUser(currentUser.getId());
        String username = request.username().trim();
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (!user.getUsername().equals(username) || !user.getEmail().equalsIgnoreCase(email)) {
            throw new BadRequestException("Username and email are login identifiers and cannot be changed from profile settings.");
        }

        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        user.setCollegeName(request.collegeName().trim());
        user.setCourse(request.course().trim());
        user.setYearOfStudy(request.yearOfStudy().trim());
        user.setCountry(request.country().trim());
        user.setBio(trimToNull(request.bio()));
        user.setGithubUrl(trimToNull(request.githubUrl()));
        user.setLinkedinUrl(trimToNull(request.linkedinUrl()));
        user.setPortfolioUrl(trimToNull(request.portfolioUrl()));
        user.setInterests(request.interests());
        user.setPublicProfileVisibility(request.publicProfileVisibility());
        return toResponse(userRepository.save(user), true);
    }

    @Transactional
    public ProfileResponse uploadProfilePicture(MultipartFile file, User currentUser) {
        if (file == null || file.isEmpty()) {
            throw new FileUploadException("Choose a profile image to upload.");
        }
        if (!ALLOWED_IMAGE_TYPES.contains(file.getContentType())) {
            throw new FileUploadException("Only JPG, PNG, and WEBP profile images are allowed.");
        }

        User user = findUser(currentUser.getId());
        user.setProfilePicturePath(imageStorageService.store(file, "profiles"));
        return toResponse(userRepository.save(user), true);
    }

    @Transactional
    public void deleteCurrent(User currentUser) {
        deleteUserAndRelations(findUser(currentUser.getId()));
    }

    @Transactional
    public ProfileResponse verifyProfile(Long userId) {
        User user = findUser(userId);
        user.setVerified(true);
        User saved = userRepository.save(user);
        notificationService.createForUser(user, NotificationType.PROFILE_VERIFIED, "Your public profile has been verified.");
        return toResponse(saved, true);
    }

    @Transactional
    public void deleteByAdmin(Long userId, User currentAdmin) {
        if (currentAdmin.getId().equals(userId)) {
            throw new BadRequestException("An administrator cannot delete their own account.");
        }
        deleteUserAndRelations(findUser(userId));
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
    }

    /**
     * Removes the direct relationships that reference a user before deleting the account.
     *
     * This explicit order is safer and easier to explain than adding broad cascade deletes to
     * User: it does not accidentally remove communities that merely contain the student, and it
     * keeps the database free of foreign-key violations when an account is deleted.
     */
    private void deleteUserAndRelations(User user) {
        Long userId = user.getId();

        // A student's membership is removed, but the shared community itself remains.
        for (Community community : communityRepository.findByMembersId(userId)) {
            community.getMembers().removeIf(member -> member.getId().equals(userId));
            communityRepository.save(community);
        }

        // Remove the student from projects they joined but do not own, then delete owned projects.
        for (Project project : projectRepository.findByMembersId(userId)) {
            if (!project.getOwner().getId().equals(userId)) {
                project.getMembers().removeIf(member -> member.getId().equals(userId));
                projectRepository.save(project);
            }
        }
        List<Project> ownedProjects = projectRepository.findAllByOwnerId(userId);
        ownedProjects.forEach(project -> reportService.resolveDeletedContent(ReportedContentType.PROJECT, project.getId()));
        ownedProjects.forEach(project -> collaborationRequestRepository.deleteByProjectId(project.getId()));
        collaborationRequestRepository.flush();
        ownedProjects.forEach(projectRepository::delete);

        roadmapRepository.findAllByOwnerId(userId).forEach(roadmapRepository::delete);
        skillRepository.findAllByUserId(userId)
                .forEach(skill -> reportService.resolveDeletedContent(ReportedContentType.SKILL, skill.getId()));
        skillRepository.deleteByUserId(userId);
        reportService.resolveDeletedContent(ReportedContentType.PROFILE, userId);
        bookmarkRepository.deleteByUserIdOrTargetUserId(userId, userId);
        collaborationRequestRepository.deleteBySenderIdOrReceiverId(userId, userId);
        notificationRepository.deleteByRecipientId(userId);
        reportRepository.deleteByReporterIdOrReportedUserId(userId, userId);
        announcementRepository.deleteByCreatedById(userId);
        // Execute child-row and join-table changes before deleting the parent account.
        userRepository.flush();
        userRepository.delete(user);
    }

    /**
     * Keeps one stable response shape for both private and public profile endpoints. Public
     * profile discovery returns null for account-only fields, while the owner and administrators
     * still receive the values they need for profile management.
     */
    private ProfileResponse toResponse(User user, boolean includePrivateFields) {
        List<Skill> skills = skillRepository.findByUserId(user.getId(), Pageable.unpaged()).getContent();
        List<String> learningSkills = skills.stream()
                .filter(skill -> skill.getIntent() == SkillIntent.LEARN)
                .map(Skill::getName)
                .toList();
        List<String> teachingSkills = skills.stream()
                .filter(skill -> skill.getIntent() == SkillIntent.TEACH)
                .map(Skill::getName)
                .toList();
        return new ProfileResponse(
                user.getId(), user.getUsername(), user.getFullName(), user.getFirstName(), user.getLastName(),
                includePrivateFields ? user.getEmail() : null,
                user.getCollegeName(), user.getCourse(), user.getYearOfStudy(), user.getCountry(),
                user.getBio(), user.getProfilePicturePath(), user.getGithubUrl(), user.getLinkedinUrl(), user.getPortfolioUrl(),
                user.getInterests(), learningSkills, teachingSkills, projectRepository.countByOwnerId(user.getId()),
                user.isPublicProfileVisibility(), user.isVerified(),
                includePrivateFields ? user.getRole() : null,
                includePrivateFields ? user.getAuthProvider() : null
        );
    }

    private String normalize(String value) {
        // PostgreSQL cannot infer a text type for null values passed through lower/concat in the
        // optional JPQL filters and binds them as bytea. An empty string keeps the parameter typed
        // as text while still representing "no filter" in UserRepository.searchPublicProfiles.
        return value == null || value.isBlank() ? "" : value.trim();
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

}
