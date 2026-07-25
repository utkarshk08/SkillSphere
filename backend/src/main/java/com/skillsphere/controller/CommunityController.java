package com.skillsphere.controller;

import com.skillsphere.domain.User;
import com.skillsphere.dto.content.CommunityRequest;
import com.skillsphere.dto.content.CommunityResponse;
import com.skillsphere.dto.content.ProjectSummaryResponse;
import com.skillsphere.dto.content.UserSummaryResponse;
import com.skillsphere.service.content.CommunityService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
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
 * Community REST API. Listing, joining, and leaving are student actions; the three
 * write endpoints for shared community details use Spring Security's standard
 * method-level role check so only ROLE_ADMIN can manage the directory.
 */
@RestController
@RequestMapping("/api/communities")
public class CommunityController {

    private final CommunityService communityService;

    public CommunityController(CommunityService communityService) {
        this.communityService = communityService;
    }

    @GetMapping
    public Page<CommunityResponse> getCommunities(
            @RequestParam(required = false) String search,
            @PageableDefault(size = 10, sort = "id") Pageable pageable,
            @AuthenticationPrincipal User currentUser
    ) {
        return communityService.getCommunities(search, pageable, currentUser);
    }

    @GetMapping("/{communityId}")
    public CommunityResponse getCommunity(
            @PathVariable Long communityId,
            @AuthenticationPrincipal User currentUser
    ) {
        return communityService.getCommunity(communityId, currentUser);
    }

    @GetMapping("/{communityId}/members")
    public Page<UserSummaryResponse> getMembers(
            @PathVariable Long communityId,
            @PageableDefault(size = 10, sort = "username") Pageable pageable
    ) {
        return communityService.getMembers(communityId, pageable);
    }

    @GetMapping("/{communityId}/projects")
    public Page<ProjectSummaryResponse> getProjects(
            @PathVariable Long communityId,
            @PageableDefault(size = 10, sort = "id") Pageable pageable
    ) {
        return communityService.getProjects(communityId, pageable);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public CommunityResponse createCommunity(
            @Valid @RequestBody CommunityRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        return communityService.create(request, currentUser);
    }

    @PutMapping("/{communityId}")
    @PreAuthorize("hasRole('ADMIN')")
    public CommunityResponse updateCommunity(
            @PathVariable Long communityId,
            @Valid @RequestBody CommunityRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        return communityService.update(communityId, request, currentUser);
    }

    @DeleteMapping("/{communityId}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCommunity(@PathVariable Long communityId) {
        communityService.delete(communityId);
    }

    @PostMapping("/{communityId}/join")
    public CommunityResponse joinCommunity(
            @PathVariable Long communityId,
            @AuthenticationPrincipal User currentUser
    ) {
        return communityService.join(communityId, currentUser);
    }

    @DeleteMapping("/{communityId}/leave")
    public CommunityResponse leaveCommunity(
            @PathVariable Long communityId,
            @AuthenticationPrincipal User currentUser
    ) {
        return communityService.leave(communityId, currentUser);
    }
}
