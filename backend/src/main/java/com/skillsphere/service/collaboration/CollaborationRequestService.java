package com.skillsphere.service.collaboration;

import com.skillsphere.domain.CollaborationRequest;
import com.skillsphere.domain.CollaborationRequestStatus;
import com.skillsphere.domain.NotificationType;
import com.skillsphere.domain.Role;
import com.skillsphere.domain.User;
import com.skillsphere.dto.collaboration.CollaborationRequestCreateRequest;
import com.skillsphere.dto.collaboration.CollaborationRequestResponse;
import com.skillsphere.dto.collaboration.CollaborationRequestStatusUpdateRequest;
import com.skillsphere.exception.BadRequestException;
import com.skillsphere.exception.ResourceNotFoundException;
import com.skillsphere.exception.UnauthorizedException;
import com.skillsphere.repository.CollaborationRequestRepository;
import com.skillsphere.repository.UserRepository;
import com.skillsphere.service.notification.NotificationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Manages the short PENDING -> ACCEPTED/REJECTED collaboration workflow.
 * The receiver changes the state, which prevents a sender from accepting their own request.
 */
@Service
public class CollaborationRequestService {

    private final CollaborationRequestRepository requestRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public CollaborationRequestService(
            CollaborationRequestRepository requestRepository,
            UserRepository userRepository,
            NotificationService notificationService
    ) {
        this.requestRepository = requestRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public Page<CollaborationRequestResponse> getMine(User currentUser, String search, Pageable pageable) {
        String normalizedSearch = search == null || search.isBlank() ? null : search.trim();
        return requestRepository.findForUser(currentUser.getId(), normalizedSearch, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public CollaborationRequestResponse getById(Long requestId, User currentUser) {
        CollaborationRequest collaborationRequest = findRequest(requestId);
        boolean involved = collaborationRequest.getSender().getId().equals(currentUser.getId())
                || collaborationRequest.getReceiver().getId().equals(currentUser.getId());
        if (!involved && currentUser.getRole() != Role.ROLE_ADMIN) {
            throw new UnauthorizedException("You cannot view this collaboration request.");
        }
        return toResponse(collaborationRequest);
    }

    @Transactional
    public CollaborationRequestResponse create(CollaborationRequestCreateRequest request, User sender) {
        if (sender.getId().equals(request.receiverId())) {
            throw new BadRequestException("You cannot send a collaboration request to yourself.");
        }

        User receiver = userRepository.findById(request.receiverId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + request.receiverId()));

        if (requestRepository.existsBySenderIdAndReceiverIdAndStatus(
                sender.getId(), receiver.getId(), CollaborationRequestStatus.PENDING)) {
            throw new BadRequestException("A pending collaboration request already exists for this student.");
        }

        CollaborationRequest collaborationRequest = new CollaborationRequest();
        collaborationRequest.setSender(sender);
        collaborationRequest.setReceiver(receiver);
        collaborationRequest.setMessage(request.message().trim());
        CollaborationRequest saved = requestRepository.save(collaborationRequest);

        notificationService.createForUser(
                receiver,
                NotificationType.COLLABORATION_REQUEST,
                "New collaboration request from " + sender.getUsername() + "."
        );
        return toResponse(saved);
    }

    @Transactional
    public CollaborationRequestResponse updateStatus(
            Long requestId,
            CollaborationRequestStatusUpdateRequest update,
            User currentUser
    ) {
        CollaborationRequest collaborationRequest = findRequest(requestId);
        if (!collaborationRequest.getReceiver().getId().equals(currentUser.getId())
                && currentUser.getRole() != Role.ROLE_ADMIN) {
            throw new UnauthorizedException("Only the request receiver can accept or reject it.");
        }
        if (update.status() == CollaborationRequestStatus.PENDING) {
            throw new BadRequestException("A request can only be accepted or rejected.");
        }
        if (collaborationRequest.getStatus() != CollaborationRequestStatus.PENDING) {
            throw new BadRequestException("This collaboration request has already been processed.");
        }

        collaborationRequest.setStatus(update.status());
        CollaborationRequest saved = requestRepository.save(collaborationRequest);
        String result = update.status() == CollaborationRequestStatus.ACCEPTED ? "accepted" : "rejected";
        notificationService.createForUser(
                collaborationRequest.getSender(),
                NotificationType.PROJECT_REQUEST_ACCEPTED,
                collaborationRequest.getReceiver().getUsername() + " " + result + " your collaboration request."
        );
        return toResponse(saved);
    }

    @Transactional
    public void delete(Long requestId, User currentUser) {
        CollaborationRequest collaborationRequest = findRequest(requestId);
        boolean involved = collaborationRequest.getSender().getId().equals(currentUser.getId())
                || collaborationRequest.getReceiver().getId().equals(currentUser.getId());
        if (!involved && currentUser.getRole() != Role.ROLE_ADMIN) {
            throw new UnauthorizedException("You cannot delete this collaboration request.");
        }
        requestRepository.delete(collaborationRequest);
    }

    private CollaborationRequest findRequest(Long requestId) {
        return requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Collaboration request not found: " + requestId));
    }

    private CollaborationRequestResponse toResponse(CollaborationRequest request) {
        return new CollaborationRequestResponse(
                request.getId(),
                request.getSender().getId(),
                request.getSender().getUsername(),
                request.getSender().getFullName(),
                request.getReceiver().getId(),
                request.getReceiver().getUsername(),
                request.getReceiver().getFullName(),
                request.getMessage(),
                request.getStatus(),
                request.getCreatedAt()
        );
    }
}
