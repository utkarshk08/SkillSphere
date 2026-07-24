package com.skillsphere.controller;

import com.skillsphere.domain.User;
import com.skillsphere.dto.collaboration.CollaborationRequestCreateRequest;
import com.skillsphere.dto.collaboration.CollaborationRequestResponse;
import com.skillsphere.dto.collaboration.CollaborationRequestStatusUpdateRequest;
import com.skillsphere.dto.common.MessageResponse;
import com.skillsphere.service.collaboration.CollaborationRequestService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Direct collaboration requests with search, pagination, and accept/reject actions. */
@RestController
@RequestMapping("/api/collaboration-requests")
@Tag(name = "Collaboration Requests")
public class CollaborationRequestController {

    private final CollaborationRequestService collaborationRequestService;

    public CollaborationRequestController(CollaborationRequestService collaborationRequestService) {
        this.collaborationRequestService = collaborationRequestService;
    }

    @GetMapping
    public Page<CollaborationRequestResponse> getMine(
            @AuthenticationPrincipal User currentUser,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable
    ) {
        return collaborationRequestService.getMine(currentUser, search, pageable);
    }

    @GetMapping("/{requestId}")
    public CollaborationRequestResponse getById(@PathVariable Long requestId, @AuthenticationPrincipal User currentUser) {
        return collaborationRequestService.getById(requestId, currentUser);
    }

    @PostMapping
    public CollaborationRequestResponse create(
            @Valid @RequestBody CollaborationRequestCreateRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        return collaborationRequestService.create(request, currentUser);
    }

    @PutMapping("/{requestId}")
    public CollaborationRequestResponse updateStatus(
            @PathVariable Long requestId,
            @Valid @RequestBody CollaborationRequestStatusUpdateRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        return collaborationRequestService.updateStatus(requestId, request, currentUser);
    }

    @DeleteMapping("/{requestId}")
    public MessageResponse delete(@PathVariable Long requestId, @AuthenticationPrincipal User currentUser) {
        collaborationRequestService.delete(requestId, currentUser);
        return new MessageResponse("Collaboration request deleted successfully.");
    }
}
