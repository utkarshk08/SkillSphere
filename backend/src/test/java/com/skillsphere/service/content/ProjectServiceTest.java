package com.skillsphere.service.content;

import com.skillsphere.domain.Project;
import com.skillsphere.domain.User;
import com.skillsphere.repository.CollaborationRequestRepository;
import com.skillsphere.repository.CommunityRepository;
import com.skillsphere.repository.ProjectRepository;
import com.skillsphere.repository.UserRepository;
import com.skillsphere.service.report.ReportService;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.util.Optional;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProjectServiceTest {

    @Test
    void deletingAProjectDeletesAndFlushesItsApplicationsFirst() {
        ProjectRepository projectRepository = mock(ProjectRepository.class);
        CollaborationRequestRepository requestRepository = mock(CollaborationRequestRepository.class);
        ReportService reportService = mock(ReportService.class);
        ProjectService service = new ProjectService(
                projectRepository,
                mock(CommunityRepository.class),
                mock(UserRepository.class),
                requestRepository,
                reportService,
                "target/test-uploads"
        );
        User owner = new User();
        owner.setId(1L);
        Project project = new Project();
        project.setId(10L);
        project.setOwner(owner);
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));

        service.delete(project.getId(), owner);

        InOrder deletionOrder = inOrder(requestRepository, projectRepository);
        deletionOrder.verify(requestRepository).deleteByProjectId(project.getId());
        deletionOrder.verify(requestRepository).flush();
        deletionOrder.verify(projectRepository).delete(project);
    }
}
