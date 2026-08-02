package com.skillsphere.service.collaboration;

import com.skillsphere.domain.CollaborationRequest;
import com.skillsphere.domain.CollaborationRequestStatus;
import com.skillsphere.domain.NotificationType;
import com.skillsphere.domain.Project;
import com.skillsphere.domain.ProjectStatus;
import com.skillsphere.domain.Role;
import com.skillsphere.domain.User;
import com.skillsphere.dto.collaboration.CollaborationRequestCreateRequest;
import com.skillsphere.dto.collaboration.CollaborationRequestResponse;
import com.skillsphere.dto.collaboration.CollaborationRequestStatusUpdateRequest;
import com.skillsphere.exception.BadRequestException;
import com.skillsphere.exception.ResourceNotFoundException;
import com.skillsphere.exception.UnauthorizedException;
import com.skillsphere.repository.CollaborationRequestRepository;
import com.skillsphere.repository.ProjectRepository;
import com.skillsphere.repository.UserRepository;
import com.skillsphere.service.notification.NotificationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/**
 * Manages both direct collaboration requests and project applications through the
 * same short PENDING -> ACCEPTED/REJECTED workflow. Accepting a project application
 * adds the sender to the project in the same database transaction.
 */
@Service
public class CollaborationRequestService {

    private final CollaborationRequestRepository requestRepository;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final NotificationService notificationService;

    public CollaborationRequestService(
            CollaborationRequestRepository requestRepository,
            UserRepository userRepository,
            ProjectRepository projectRepository,
            NotificationService notificationService
    ) {
        this.requestRepository = requestRepository;
        this.userRepository = userRepository;
        this.projectRepository = projectRepository;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public Page<CollaborationRequestResponse> getMine(User currentUser, String search, Pageable pageable) {
        String normalizedSearch = search == null || search.isBlank() ? "" : search.trim();
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
        boolean hasReceiver = request.receiverId() != null;
        boolean hasProject = request.projectId() != null;
        if (hasReceiver == hasProject) {
            throw new BadRequestException("Choose either a student or a project, but not both.");
        }

        Project project = null;
        User receiver;
        if (hasProject) {
            project = projectRepository.findById(request.projectId())
                    .orElseThrow(() -> new ResourceNotFoundException("Project not found: " + request.projectId()));
            validateProjectApplication(project, sender);
            receiver = project.getOwner();

            if (requestRepository.existsBySenderIdAndProjectIdAndStatus(
                    sender.getId(), project.getId(), CollaborationRequestStatus.PENDING)) {
                throw new BadRequestException("You already have a pending application for this project.");
            }
        } else {
            if (sender.getId().equals(request.receiverId())) {
                throw new BadRequestException("You cannot send a collaboration request to yourself.");
            }
            receiver = userRepository.findById(request.receiverId())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found: " + request.receiverId()));

            if (requestRepository.existsBySenderIdAndReceiverIdAndStatusAndProjectIsNull(
                    sender.getId(), receiver.getId(), CollaborationRequestStatus.PENDING)) {
                throw new BadRequestException("A pending collaboration request already exists for this student.");
            }
        }

        CollaborationRequest collaborationRequest = new CollaborationRequest();
        collaborationRequest.setSender(sender);
        collaborationRequest.setReceiver(receiver);
        collaborationRequest.setProject(project);
        collaborationRequest.setMessage(request.message().trim());
        CollaborationRequest saved = requestRepository.save(collaborationRequest);

        String notificationMessage = project == null
                ? "New collaboration request from " + sender.getUsername() + "."
                : sender.getUsername() + " applied to join your project \"" + project.getTitle() + "\".";
        notificationService.createForUser(
                receiver,
                NotificationType.COLLABORATION_REQUEST,
                notificationMessage
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
        boolean isReceiver = collaborationRequest.getReceiver().getId().equals(currentUser.getId());
        boolean adminCanManageGeneralRequest = collaborationRequest.getProject() == null
                && currentUser.getRole() == Role.ROLE_ADMIN;
        if (!isReceiver && !adminCanManageGeneralRequest) {
            throw new UnauthorizedException("Only the request receiver can accept or reject it.");
        }
        if (update.status() == CollaborationRequestStatus.PENDING) {
            throw new BadRequestException("A request can only be accepted or rejected.");
        }
        if (collaborationRequest.getStatus() != CollaborationRequestStatus.PENDING) {
            throw new BadRequestException("This collaboration request has already been processed.");
        }

        Project project = collaborationRequest.getProject();
        if (update.status() == CollaborationRequestStatus.ACCEPTED && project != null) {
            validateProjectApplication(project, collaborationRequest.getSender());
            project.getMembers().add(collaborationRequest.getSender());
            projectRepository.save(project);
        }

        collaborationRequest.setStatus(update.status());
        collaborationRequest.setResponseMessage(trimToNull(update.responseMessage()));
        CollaborationRequest saved = requestRepository.save(collaborationRequest);
        String result = update.status() == CollaborationRequestStatus.ACCEPTED ? "accepted" : "rejected";
        String responseHint = saved.getResponseMessage() == null
                ? ""
                : " Open collaboration requests to read their response.";
        String notificationMessage = project == null
                ? collaborationRequest.getReceiver().getUsername() + " " + result
                        + " your collaboration request." + responseHint
                : collaborationRequest.getReceiver().getUsername() + " " + result
                        + " your application for project \"" + project.getTitle() + "\"." + responseHint;
        notificationService.createForUser(
                collaborationRequest.getSender(),
                update.status() == CollaborationRequestStatus.ACCEPTED
                        ? NotificationType.PROJECT_REQUEST_ACCEPTED
                        : NotificationType.GENERAL,
                notificationMessage
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

    private void validateProjectApplication(Project project, User applicant) {
        if (project.getStatus() != ProjectStatus.OPEN) {
            throw new BadRequestException("This project is not open for applications.");
        }
        if (project.getOwner().getId().equals(applicant.getId())) {
            throw new BadRequestException("The project owner is already a member.");
        }
        if (containsMember(project, applicant.getId())) {
            throw new BadRequestException("You are already a member of this project.");
        }
        if (project.getMaximumMembers() == null
                || project.getCurrentMemberCount() >= project.getMaximumMembers()) {
            throw new BadRequestException("This project has no open member positions.");
        }
    }

    private boolean containsMember(Project project, Long userId) {
        return project.getMembers().stream()
                .anyMatch(member -> Objects.equals(member.getId(), userId));
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private CollaborationRequestResponse toResponse(CollaborationRequest request) {
        Project project = request.getProject();
        return new CollaborationRequestResponse(
                request.getId(),
                request.getSender().getId(),
                request.getSender().getUsername(),
                request.getSender().getFullName(),
                request.getReceiver().getId(),
                request.getReceiver().getUsername(),
                request.getReceiver().getFullName(),
                project == null ? null : project.getId(),
                project == null ? null : project.getTitle(),
                request.getMessage(),
                request.getResponseMessage(),
                request.getStatus(),
                request.getCreatedAt()
        );
    }
}
