package com.skillsphere.service.profile;

import com.skillsphere.domain.AuthProvider;
import com.skillsphere.domain.Role;
import com.skillsphere.domain.User;
import com.skillsphere.dto.profile.ProfileResponse;
import com.skillsphere.repository.AnnouncementRepository;
import com.skillsphere.repository.BookmarkRepository;
import com.skillsphere.repository.CollaborationRequestRepository;
import com.skillsphere.repository.CommunityRepository;
import com.skillsphere.repository.NotificationRepository;
import com.skillsphere.repository.ProjectRepository;
import com.skillsphere.repository.ReportRepository;
import com.skillsphere.repository.RoadmapRepository;
import com.skillsphere.repository.SkillRepository;
import com.skillsphere.repository.UserRepository;
import com.skillsphere.service.notification.NotificationService;
import com.skillsphere.service.report.ReportService;
import com.skillsphere.service.storage.ImageStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProfileServiceTest {

    private UserRepository userRepository;
    private SkillRepository skillRepository;
    private ProjectRepository projectRepository;
    private ImageStorageService imageStorageService;
    private ProfileService profileService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        skillRepository = mock(SkillRepository.class);
        projectRepository = mock(ProjectRepository.class);
        imageStorageService = mock(ImageStorageService.class);
        profileService = new ProfileService(
                userRepository,
                skillRepository,
                projectRepository,
                mock(CommunityRepository.class),
                mock(RoadmapRepository.class),
                mock(BookmarkRepository.class),
                mock(CollaborationRequestRepository.class),
                mock(NotificationRepository.class),
                mock(ReportRepository.class),
                mock(AnnouncementRepository.class),
                mock(NotificationService.class),
                mock(ReportService.class),
                imageStorageService
        );
    }

    @Test
    void searchReturnsOnlyTheStudentRoleAndRedactsAccountFields() {
        User student = student(1L, "public_student");
        Pageable pageable = PageRequest.of(0, 10);
        when(userRepository.searchPublicProfiles(
                null, null, null, null, null, Role.ROLE_USER, pageable
        )).thenReturn(new PageImpl<>(List.of(student)));
        stubProfileDetails(student);

        Page<ProfileResponse> result = profileService.search(
                null, null, null, null, null, pageable
        );

        ProfileResponse response = result.getContent().getFirst();
        assertNull(response.email());
        assertNull(response.role());
        assertNull(response.authProvider());
        verify(userRepository).searchPublicProfiles(
                null, null, null, null, null, Role.ROLE_USER, pageable
        );
    }

    @Test
    void anonymousPublicProfileDoesNotExposeAccountFields() {
        User student = student(1L, "public_student");
        when(userRepository.findByUsernameIgnoreCase(student.getUsername())).thenReturn(Optional.of(student));
        stubProfileDetails(student);

        ProfileResponse response = profileService.getByUsername(student.getUsername(), null);

        assertNull(response.email());
        assertNull(response.role());
        assertNull(response.authProvider());
    }

    @Test
    void anotherStudentDoesNotSeeAccountFieldsOnAPublicProfile() {
        User student = student(1L, "public_student");
        User viewer = student(2L, "other_student");
        when(userRepository.findByUsernameIgnoreCase(student.getUsername())).thenReturn(Optional.of(student));
        stubProfileDetails(student);

        ProfileResponse response = profileService.getByUsername(student.getUsername(), viewer);

        assertNull(response.email());
        assertNull(response.role());
        assertNull(response.authProvider());
    }

    @Test
    void profileOwnerRetainsPrivateAccountFields() {
        User student = student(1L, "profile_owner");
        when(userRepository.findById(student.getId())).thenReturn(Optional.of(student));
        stubProfileDetails(student);

        ProfileResponse response = profileService.getCurrent(student);

        assertEquals(student.getEmail(), response.email());
        assertEquals(Role.ROLE_USER, response.role());
        assertEquals(AuthProvider.LOCAL, response.authProvider());
    }

    @Test
    void administratorRetainsPrivateAccountFields() {
        User student = student(1L, "public_student");
        User admin = student(2L, "site_admin");
        admin.setRole(Role.ROLE_ADMIN);
        when(userRepository.findByUsernameIgnoreCase(student.getUsername())).thenReturn(Optional.of(student));
        stubProfileDetails(student);

        ProfileResponse response = profileService.getByUsername(student.getUsername(), admin);

        assertEquals(student.getEmail(), response.email());
        assertEquals(Role.ROLE_USER, response.role());
        assertEquals(AuthProvider.LOCAL, response.authProvider());
    }

    @Test
    void uploadedProfilePicturePersistsTheStorageProviderUrl() {
        User student = student(1L, "profile_owner");
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "avatar.png",
                "image/png",
                new byte[]{1, 2, 3}
        );
        String cloudUrl = "https://res.cloudinary.com/demo/image/upload/profile.png";
        when(userRepository.findById(student.getId())).thenReturn(Optional.of(student));
        when(imageStorageService.store(file, "profiles")).thenReturn(cloudUrl);
        when(userRepository.save(student)).thenReturn(student);
        stubProfileDetails(student);

        ProfileResponse response = profileService.uploadProfilePicture(file, student);

        assertEquals(cloudUrl, response.profilePicturePath());
        verify(imageStorageService).store(file, "profiles");
    }

    private User student(Long id, String username) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setEmail(username + "@example.com");
        user.setFirstName("Test");
        user.setLastName("Student");
        user.setPublicProfileVisibility(true);
        user.setRole(Role.ROLE_USER);
        user.setAuthProvider(AuthProvider.LOCAL);
        return user;
    }

    private void stubProfileDetails(User user) {
        when(skillRepository.findByUserId(user.getId(), Pageable.unpaged())).thenReturn(Page.empty());
        when(projectRepository.countByOwnerId(user.getId())).thenReturn(0L);
    }
}
