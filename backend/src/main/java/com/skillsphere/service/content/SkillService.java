package com.skillsphere.service.content;

import com.skillsphere.domain.Skill;
import com.skillsphere.domain.Role;
import com.skillsphere.domain.ReportedContentType;
import com.skillsphere.domain.User;
import com.skillsphere.dto.content.SkillRequest;
import com.skillsphere.dto.content.SkillResponse;
import com.skillsphere.exception.ResourceNotFoundException;
import com.skillsphere.exception.UnauthorizedException;
import com.skillsphere.repository.SkillRepository;
import com.skillsphere.service.report.ReportService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Holds the business rules for the personal Skills module.
 *
 * Controllers only translate HTTP requests here, while this service checks ownership
 * before changing a record and the repository persists it. This explicit
 * controller-service-repository flow is intentional: it keeps authorization and
 * database logic out of controllers, and it is straightforward to explain in an
 * interview. Search returns a Page rather than every skill so large student lists do
 * not become expensive to transfer or render.
 */
@Service
public class SkillService {

    private final SkillRepository skillRepository;
    private final ReportService reportService;

    public SkillService(SkillRepository skillRepository, ReportService reportService) {
        this.skillRepository = skillRepository;
        this.reportService = reportService;
    }

    @Transactional(readOnly = true)
    public Page<SkillResponse> getSkills(String search, Pageable pageable) {
        String normalizedSearch = normalizeSearch(search);
        Page<Skill> skills = normalizedSearch == null
                ? skillRepository.findAll(pageable)
                : skillRepository.findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
                        normalizedSearch,
                        normalizedSearch,
                        pageable
                );
        return skills.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public SkillResponse getSkill(Long skillId) {
        return toResponse(findSkill(skillId));
    }

    @Transactional(readOnly = true)
    public Page<SkillResponse> getMySkills(User currentUser, Pageable pageable) {
        User user = requireCurrentUser(currentUser);
        return skillRepository.findByUserId(user.getId(), pageable).map(this::toResponse);
    }

    @Transactional
    public SkillResponse create(SkillRequest request, User currentUser) {
        User user = requireCurrentUser(currentUser);
        Skill skill = new Skill();
        applyRequest(skill, request);
        skill.setUser(user);
        return toResponse(skillRepository.save(skill));
    }

    @Transactional
    public SkillResponse update(Long skillId, SkillRequest request, User currentUser) {
        Skill skill = findSkill(skillId);
        ensureOwnerOrAdmin(skill, currentUser);
        applyRequest(skill, request);
        return toResponse(skillRepository.save(skill));
    }

    @Transactional
    public void delete(Long skillId, User currentUser) {
        Skill skill = findSkill(skillId);
        ensureOwnerOrAdmin(skill, currentUser);
        skillRepository.delete(skill);
        reportService.resolveDeletedContent(ReportedContentType.SKILL, skillId);
    }

    private Skill findSkill(Long skillId) {
        return skillRepository.findById(skillId)
                .orElseThrow(() -> new ResourceNotFoundException("Skill not found: " + skillId));
    }

    /** Admins can manage inappropriate skill entries; students only manage their own. */
    private void ensureOwnerOrAdmin(Skill skill, User currentUser) {
        User user = requireCurrentUser(currentUser);
        if (!skill.getUser().getId().equals(user.getId()) && user.getRole() != Role.ROLE_ADMIN) {
            throw new UnauthorizedException("You can only manage your own skills.");
        }
    }

    private User requireCurrentUser(User currentUser) {
        if (currentUser == null || currentUser.getId() == null) {
            throw new UnauthorizedException("Authentication is required for this action.");
        }
        return currentUser;
    }

    private void applyRequest(Skill skill, SkillRequest request) {
        skill.setName(request.name().trim());
        skill.setLevel(request.level());
        skill.setIntent(request.intent());
        skill.setDescription(request.description().trim());
        skill.setExperienceMonths(request.experienceMonths());
    }

    private SkillResponse toResponse(Skill skill) {
        return new SkillResponse(
                skill.getId(),
                skill.getName(),
                skill.getLevel(),
                skill.getIntent(),
                skill.getDescription(),
                skill.getExperienceMonths(),
                skill.getUser().getId(),
                skill.getUser().getUsername()
        );
    }

    private String normalizeSearch(String search) {
        return StringUtils.hasText(search) ? search.trim() : null;
    }
}
