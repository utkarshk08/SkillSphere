package com.skillsphere.controller;

import com.skillsphere.domain.User;
import com.skillsphere.dto.content.SkillRequest;
import com.skillsphere.dto.content.SkillResponse;
import com.skillsphere.service.content.SkillService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
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

/**
 * REST entry point for a student's personal skills.
 *
 * The controller deliberately does only HTTP work: it validates request bodies,
 * receives Pageable parameters, and passes the authenticated User to SkillService.
 * The service owns authorization and business rules, preserving the requested
 * Controller -> Service -> Repository architecture.
 */
@RestController
@RequestMapping("/api/skills")
public class SkillController {

    private final SkillService skillService;

    public SkillController(SkillService skillService) {
        this.skillService = skillService;
    }

    @GetMapping
    public Page<SkillResponse> getSkills(
            @RequestParam(required = false) String search,
            @PageableDefault(size = 10, sort = "id") Pageable pageable
    ) {
        return skillService.getSkills(search, pageable);
    }

    @GetMapping("/mine")
    public Page<SkillResponse> getMySkills(
            @AuthenticationPrincipal User currentUser,
            @PageableDefault(size = 10, sort = "id") Pageable pageable
    ) {
        return skillService.getMySkills(currentUser, pageable);
    }

    @GetMapping("/{skillId}")
    public SkillResponse getSkill(@PathVariable Long skillId) {
        return skillService.getSkill(skillId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SkillResponse createSkill(
            @Valid @RequestBody SkillRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        return skillService.create(request, currentUser);
    }

    @PutMapping("/{skillId}")
    public SkillResponse updateSkill(
            @PathVariable Long skillId,
            @Valid @RequestBody SkillRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        return skillService.update(skillId, request, currentUser);
    }

    @DeleteMapping("/{skillId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSkill(
            @PathVariable Long skillId,
            @AuthenticationPrincipal User currentUser
    ) {
        skillService.delete(skillId, currentUser);
    }
}
