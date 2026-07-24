package com.skillsphere.controller;

import com.skillsphere.domain.User;
import com.skillsphere.dto.common.MessageResponse;
import com.skillsphere.dto.roadmap.RoadmapRequest;
import com.skillsphere.dto.roadmap.RoadmapResponse;
import com.skillsphere.service.roadmap.RoadmapService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Endpoints for public/shared and private learning roadmaps. */
@RestController
@RequestMapping("/api/roadmaps")
@Tag(name = "Roadmaps", description = "Learning roadmaps with calculated progress")
public class RoadmapController {

    private final RoadmapService roadmapService;

    public RoadmapController(RoadmapService roadmapService) {
        this.roadmapService = roadmapService;
    }

    @GetMapping
    public Page<RoadmapResponse> getPublic(@PageableDefault(size = 10, sort = "createdAt") Pageable pageable) {
        return roadmapService.getPublicRoadmaps(pageable);
    }

    @GetMapping("/mine")
    public Page<RoadmapResponse> getMine(
            @AuthenticationPrincipal User currentUser,
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable
    ) {
        return roadmapService.getMyRoadmaps(currentUser, pageable);
    }

    @GetMapping("/{roadmapId}")
    public RoadmapResponse getById(@PathVariable Long roadmapId, @AuthenticationPrincipal User currentUser) {
        return roadmapService.getById(roadmapId, currentUser);
    }

    @PostMapping
    public RoadmapResponse create(@Valid @RequestBody RoadmapRequest request, @AuthenticationPrincipal User currentUser) {
        return roadmapService.create(request, currentUser);
    }

    @PutMapping("/{roadmapId}")
    public RoadmapResponse update(
            @PathVariable Long roadmapId,
            @Valid @RequestBody RoadmapRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        return roadmapService.update(roadmapId, request, currentUser);
    }

    @DeleteMapping("/{roadmapId}")
    public MessageResponse delete(@PathVariable Long roadmapId, @AuthenticationPrincipal User currentUser) {
        roadmapService.delete(roadmapId, currentUser);
        return new MessageResponse("Roadmap deleted successfully.");
    }
}
