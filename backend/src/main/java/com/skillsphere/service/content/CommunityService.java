package com.skillsphere.service.content;

import com.skillsphere.domain.Community;
import com.skillsphere.domain.Project;
import com.skillsphere.domain.NotificationType;
import com.skillsphere.domain.ReportedContentType;
import com.skillsphere.domain.User;
import com.skillsphere.dto.content.CommunityRequest;
import com.skillsphere.dto.content.CommunityResponse;
import com.skillsphere.dto.content.ProjectSummaryResponse;
import com.skillsphere.dto.content.UserSummaryResponse;
import com.skillsphere.exception.BadRequestException;
import com.skillsphere.exception.ResourceNotFoundException;
import com.skillsphere.exception.UnauthorizedException;
import com.skillsphere.repository.CommunityRepository;
import com.skillsphere.repository.ProjectRepository;
import com.skillsphere.repository.BookmarkRepository;
import com.skillsphere.service.notification.NotificationService;
import com.skillsphere.service.report.ReportService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Provides the community directory, membership actions, and administrator CRUD.
 *
 * Administrators create the shared community content through controller-level role
 * checks. Students can only join or leave, which gives a clear and safe separation
 * of responsibilities without introducing a complicated community-admin role model.
 * Paginated directory and member views demonstrate Spring Data's Pageable support
 * instead of returning every community or member at once.
 */
@Service
public class CommunityService {

    private final CommunityRepository communityRepository;
    private final ProjectRepository projectRepository;
    private final BookmarkRepository bookmarkRepository;
    private final NotificationService notificationService;
    private final ReportService reportService;

    public CommunityService(
            CommunityRepository communityRepository,
            ProjectRepository projectRepository,
            BookmarkRepository bookmarkRepository,
            NotificationService notificationService,
            ReportService reportService
    ) {
        this.communityRepository = communityRepository;
        this.projectRepository = projectRepository;
        this.bookmarkRepository = bookmarkRepository;
        this.notificationService = notificationService;
        this.reportService = reportService;
    }

    @Transactional(readOnly = true)
    public Page<CommunityResponse> getCommunities(String search, Pageable pageable, User currentUser) {
        String normalizedSearch = normalizeSearch(search);
        Page<Community> communities = normalizedSearch == null
                ? communityRepository.findAll(pageable)
                : communityRepository.findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
                        normalizedSearch,
                        normalizedSearch,
                        pageable
                );
        return communities.map(community -> toResponse(community, currentUser));
    }

    @Transactional(readOnly = true)
    public CommunityResponse getCommunity(Long communityId, User currentUser) {
        return toResponse(findCommunity(communityId), currentUser);
    }

    @Transactional(readOnly = true)
    public Page<UserSummaryResponse> getMembers(Long communityId, Pageable pageable) {
        Community community = findCommunity(communityId);
        List<User> members = community.getMembers().stream()
                .sorted(Comparator.comparing(User::getUsername, String.CASE_INSENSITIVE_ORDER))
                .toList();

        int totalMembers = members.size();
        int start = (int) Math.min(pageable.getOffset(), totalMembers);
        int end = Math.min(start + pageable.getPageSize(), totalMembers);
        List<UserSummaryResponse> content = members.subList(start, end).stream()
                .map(this::toUserSummary)
                .toList();
        return new PageImpl<>(content, pageable, totalMembers);
    }

    @Transactional(readOnly = true)
    public Page<ProjectSummaryResponse> getProjects(Long communityId, Pageable pageable) {
        findCommunity(communityId);
        return projectRepository.findByCommunityId(communityId, pageable).map(this::toProjectSummary);
    }

    @Transactional
    public CommunityResponse create(CommunityRequest request, User currentUser) {
        String communityName = request.name().trim();
        if (communityRepository.existsByNameIgnoreCase(communityName)) {
            throw new BadRequestException("A community with this name already exists.");
        }

        Community community = new Community();
        applyRequest(community, request);
        return toResponse(communityRepository.save(community), currentUser);
    }

    @Transactional
    public CommunityResponse update(Long communityId, CommunityRequest request, User currentUser) {
        Community community = findCommunity(communityId);
        String requestedName = request.name().trim();
        if (!community.getName().equalsIgnoreCase(requestedName)
                && communityRepository.existsByNameIgnoreCase(requestedName)) {
            throw new BadRequestException("A community with this name already exists.");
        }

        applyRequest(community, request);
        return toResponse(communityRepository.save(community), currentUser);
    }

    @Transactional
    public void delete(Long communityId) {
        Community community = findCommunity(communityId);

        // Projects can outlive a community, so clear their optional association first.
        List<Project> communityProjects = projectRepository.findByCommunityId(communityId);
        communityProjects.forEach(project -> project.setCommunity(null));
        projectRepository.saveAll(communityProjects);

        // Community bookmarks point at this entity and must be cleared before its FK is removed.
        bookmarkRepository.deleteByTargetCommunityId(communityId);

        communityRepository.delete(community);
        reportService.resolveDeletedContent(ReportedContentType.COMMUNITY, communityId);
    }

    @Transactional
    public CommunityResponse join(Long communityId, User currentUser) {
        Community community = findCommunity(communityId);
        User user = requireCurrentUser(currentUser);
        if (containsMember(community, user.getId())) {
            throw new BadRequestException("You have already joined this community.");
        }

        community.getMembers().add(user);
        Community saved = communityRepository.save(community);
        notificationService.createForUser(
                user,
                NotificationType.COMMUNITY_JOINED,
                "You joined " + saved.getName() + " successfully."
        );
        return toResponse(saved, user);
    }

    @Transactional
    public CommunityResponse leave(Long communityId, User currentUser) {
        Community community = findCommunity(communityId);
        User user = requireCurrentUser(currentUser);
        if (!containsMember(community, user.getId())) {
            throw new BadRequestException("You are not a member of this community.");
        }

        community.getMembers().removeIf(member -> Objects.equals(member.getId(), user.getId()));
        return toResponse(communityRepository.save(community), user);
    }

    private Community findCommunity(Long communityId) {
        return communityRepository.findById(communityId)
                .orElseThrow(() -> new ResourceNotFoundException("Community not found: " + communityId));
    }

    private boolean containsMember(Community community, Long userId) {
        return community.getMembers().stream().anyMatch(member -> Objects.equals(member.getId(), userId));
    }

    private User requireCurrentUser(User currentUser) {
        if (currentUser == null || currentUser.getId() == null) {
            throw new UnauthorizedException("Authentication is required for this action.");
        }
        return currentUser;
    }

    private void applyRequest(Community community, CommunityRequest request) {
        community.setName(request.name().trim());
        community.setDescription(request.description().trim());
        community.setResources(normalizeValues(request.resources()));
    }

    private Set<String> normalizeValues(Set<String> values) {
        if (values == null) {
            return new LinkedHashSet<>();
        }
        return values.stream()
                .map(String::trim)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private CommunityResponse toResponse(Community community, User currentUser) {
        boolean member = currentUser != null
                && currentUser.getId() != null
                && containsMember(community, currentUser.getId());
        return new CommunityResponse(
                community.getId(),
                community.getName(),
                community.getDescription(),
                new LinkedHashSet<>(community.getResources()),
                community.getMembers().size(),
                community.getProjects().size(),
                community.getResources().size(),
                member
        );
    }

    private ProjectSummaryResponse toProjectSummary(Project project) {
        return new ProjectSummaryResponse(
                project.getId(),
                project.getTitle(),
                project.getDescription(),
                new LinkedHashSet<>(project.getTechStack()),
                new LinkedHashSet<>(project.getRequiredSkills()),
                project.getDeadline(),
                project.getStatus(),
                project.getOwner().getUsername(),
                project.getCurrentMemberCount(),
                project.getOpenPositions()
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
