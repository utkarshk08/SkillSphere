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
import com.skillsphere.exception.UnauthorizedException;
import com.skillsphere.repository.CollaborationRequestRepository;
import com.skillsphere.repository.ProjectRepository;
import com.skillsphere.repository.UserRepository;
import com.skillsphere.service.notification.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CollaborationRequestServiceTest {

    private CollaborationRequestRepository requestRepository;
    private UserRepository userRepository;
    private ProjectRepository projectRepository;
    private NotificationService notificationService;
    private CollaborationRequestService service;

    @BeforeEach
    void setUp() {
        requestRepository = mock(CollaborationRequestRepository.class);
        userRepository = mock(UserRepository.class);
        projectRepository = mock(ProjectRepository.class);
        notificationService = mock(NotificationService.class);
        service = new CollaborationRequestService(
                requestRepository,
                userRepository,
                projectRepository,
                notificationService
        );
        when(requestRepository.save(any(CollaborationRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void blankSearchUsesAnEmptyStringForPostgresSafeBinding() {
        User currentUser = user(1L, "current");
        PageRequest pageable = PageRequest.of(0, 10);
        when(requestRepository.findForUser(currentUser.getId(), "", pageable))
                .thenReturn(Page.empty(pageable));

        service.getMine(currentUser, "   ", pageable);

        verify(requestRepository).findForUser(currentUser.getId(), "", pageable);
    }

    @Test
    void projectApplicationUsesTheProjectOwnerAsReceiver() {
        User applicant = user(1L, "applicant");
        User owner = user(2L, "owner");
        Project project = openProject(10L, "Study Planner", owner, 4);
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));

        CollaborationRequestResponse response = service.create(
                new CollaborationRequestCreateRequest(null, project.getId(), " I would like to help. "),
                applicant
        );

        ArgumentCaptor<CollaborationRequest> savedRequest = ArgumentCaptor.forClass(CollaborationRequest.class);
        verify(requestRepository).save(savedRequest.capture());
        assertEquals(owner, savedRequest.getValue().getReceiver());
        assertEquals(project, savedRequest.getValue().getProject());
        assertEquals("I would like to help.", savedRequest.getValue().getMessage());
        assertEquals(project.getId(), response.projectId());
        assertEquals(project.getTitle(), response.projectTitle());
        verify(notificationService).createForUser(
                owner,
                NotificationType.COLLABORATION_REQUEST,
                "applicant applied to join your project \"Study Planner\"."
        );
        verify(userRepository, never()).findById(any());
    }

    @Test
    void acceptingProjectApplicationAddsTheApplicantAndStoresTheReply() {
        User applicant = user(1L, "applicant");
        User owner = user(2L, "owner");
        Project project = openProject(10L, "Study Planner", owner, 4);
        CollaborationRequest request = projectRequest(20L, applicant, owner, project);
        when(requestRepository.findById(request.getId())).thenReturn(Optional.of(request));

        CollaborationRequestResponse response = service.updateStatus(
                request.getId(),
                new CollaborationRequestStatusUpdateRequest(
                        CollaborationRequestStatus.ACCEPTED,
                        " Welcome to the team! "
                ),
                owner
        );

        assertTrue(project.getMembers().contains(applicant));
        assertEquals(CollaborationRequestStatus.ACCEPTED, request.getStatus());
        assertEquals("Welcome to the team!", request.getResponseMessage());
        assertEquals("Welcome to the team!", response.responseMessage());
        verify(projectRepository).save(project);
        verify(notificationService).createForUser(
                applicant,
                NotificationType.PROJECT_REQUEST_ACCEPTED,
                "owner accepted your application for project \"Study Planner\"."
                        + " Open collaboration requests to read their response."
        );
    }

    @Test
    void rejectingProjectApplicationStoresReplyWithoutAddingMember() {
        User applicant = user(1L, "applicant");
        User owner = user(2L, "owner");
        Project project = openProject(10L, "Study Planner", owner, 4);
        CollaborationRequest request = projectRequest(20L, applicant, owner, project);
        when(requestRepository.findById(request.getId())).thenReturn(Optional.of(request));

        service.updateStatus(
                request.getId(),
                new CollaborationRequestStatusUpdateRequest(
                        CollaborationRequestStatus.REJECTED,
                        " We need a designer right now. "
                ),
                owner
        );

        assertFalse(project.getMembers().contains(applicant));
        assertEquals("We need a designer right now.", request.getResponseMessage());
        verify(projectRepository, never()).save(any(Project.class));
        verify(notificationService).createForUser(
                applicant,
                NotificationType.GENERAL,
                "owner rejected your application for project \"Study Planner\"."
                        + " Open collaboration requests to read their response."
        );
    }

    @Test
    void projectOwnerAloneCanProcessAProjectApplication() {
        User applicant = user(1L, "applicant");
        User owner = user(2L, "owner");
        User unrelatedAdmin = user(3L, "admin");
        unrelatedAdmin.setRole(Role.ROLE_ADMIN);
        Project project = openProject(10L, "Study Planner", owner, 4);
        CollaborationRequest request = projectRequest(20L, applicant, owner, project);
        when(requestRepository.findById(request.getId())).thenReturn(Optional.of(request));

        assertThrows(
                UnauthorizedException.class,
                () -> service.updateStatus(
                        request.getId(),
                        new CollaborationRequestStatusUpdateRequest(
                                CollaborationRequestStatus.ACCEPTED,
                                null
                        ),
                        unrelatedAdmin
                )
        );

        assertFalse(project.getMembers().contains(applicant));
        verify(projectRepository, never()).save(any(Project.class));
    }

    @Test
    void acceptanceRechecksCapacityBeforeAddingTheApplicant() {
        User applicant = user(1L, "applicant");
        User owner = user(2L, "owner");
        Project project = openProject(10L, "Study Planner", owner, 1);
        CollaborationRequest request = projectRequest(20L, applicant, owner, project);
        when(requestRepository.findById(request.getId())).thenReturn(Optional.of(request));

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> service.updateStatus(
                        request.getId(),
                        new CollaborationRequestStatusUpdateRequest(
                                CollaborationRequestStatus.ACCEPTED,
                                null
                        ),
                        owner
                )
        );

        assertEquals("This project has no open member positions.", exception.getMessage());
        assertEquals(CollaborationRequestStatus.PENDING, request.getStatus());
        verify(requestRepository, never()).save(any(CollaborationRequest.class));
    }

    @Test
    void existingGeneralCollaborationRequestFlowStillWorks() {
        User sender = user(1L, "sender");
        User receiver = user(2L, "receiver");
        when(userRepository.findById(receiver.getId())).thenReturn(Optional.of(receiver));

        CollaborationRequestResponse response = service.create(
                new CollaborationRequestCreateRequest(receiver.getId(), null, "Let us practise Java."),
                sender
        );

        assertNull(response.projectId());
        assertNull(response.projectTitle());
        verify(requestRepository).existsBySenderIdAndReceiverIdAndStatusAndProjectIsNull(
                sender.getId(),
                receiver.getId(),
                CollaborationRequestStatus.PENDING
        );
        verify(requestRepository, never()).existsBySenderIdAndProjectIdAndStatus(any(), any(), any());
    }

    @Test
    void createRequiresExactlyOneTarget() {
        User sender = user(1L, "sender");

        assertThrows(
                BadRequestException.class,
                () -> service.create(
                        new CollaborationRequestCreateRequest(null, null, "Let us collaborate."),
                        sender
                )
        );
        assertThrows(
                BadRequestException.class,
                () -> service.create(
                        new CollaborationRequestCreateRequest(2L, 10L, "Let us collaborate."),
                        sender
                )
        );

        verify(requestRepository, never()).save(any(CollaborationRequest.class));
    }

    private User user(Long id, String username) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setEmail(username + "@example.com");
        user.setFirstName(username);
        user.setLastName("Student");
        user.setRole(Role.ROLE_USER);
        return user;
    }

    private Project openProject(Long id, String title, User owner, int maximumMembers) {
        Project project = new Project();
        project.setId(id);
        project.setTitle(title);
        project.setOwner(owner);
        project.setStatus(ProjectStatus.OPEN);
        project.setMaximumMembers(maximumMembers);
        project.getMembers().add(owner);
        return project;
    }

    private CollaborationRequest projectRequest(
            Long id,
            User sender,
            User receiver,
            Project project
    ) {
        CollaborationRequest request = new CollaborationRequest();
        request.setId(id);
        request.setSender(sender);
        request.setReceiver(receiver);
        request.setProject(project);
        request.setMessage("Please consider my application.");
        return request;
    }
}
