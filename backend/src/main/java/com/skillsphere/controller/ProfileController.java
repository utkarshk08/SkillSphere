package com.skillsphere.controller;

import com.skillsphere.domain.User;
import com.skillsphere.dto.common.MessageResponse;
import com.skillsphere.dto.profile.ProfileResponse;
import com.skillsphere.dto.profile.ProfileUpdateRequest;
import com.skillsphere.service.profile.ProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.multipart.MultipartFile;

/** Profile CRUD, searchable student discovery, and profile-picture upload endpoints. */
@RestController
@RequestMapping("/api/profiles")
@Tag(name = "Profiles", description = "Student profile discovery and management")
@SecurityRequirement(name = "bearerAuth")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping
    @Operation(summary = "Search public profiles by name, college, country, skills, or interests")
    public Page<ProfileResponse> search(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String college,
            @RequestParam(required = false) String country,
            @RequestParam(required = false) String skill,
            @RequestParam(required = false) String interest,
            @PageableDefault(size = 10, sort = "username") Pageable pageable
    ) {
        return profileService.search(name, college, country, skill, interest, pageable);
    }

    @GetMapping("/me")
    public ProfileResponse getCurrent(@AuthenticationPrincipal User currentUser) {
        return profileService.getCurrent(currentUser);
    }

    @GetMapping("/{username}")
    public ProfileResponse getByUsername(@PathVariable String username, @AuthenticationPrincipal User currentUser) {
        return profileService.getByUsername(username, currentUser);
    }

    @PutMapping("/me")
    public ProfileResponse update(@Valid @RequestBody ProfileUpdateRequest request, @AuthenticationPrincipal User currentUser) {
        return profileService.update(request, currentUser);
    }

    @PostMapping(value = "/me/picture", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ProfileResponse uploadPicture(
            @RequestPart("file") MultipartFile file,
            @AuthenticationPrincipal User currentUser
    ) {
        return profileService.uploadProfilePicture(file, currentUser);
    }

    @DeleteMapping("/me")
    public MessageResponse deleteCurrent(@AuthenticationPrincipal User currentUser) {
        profileService.deleteCurrent(currentUser);
        return new MessageResponse("Profile deleted successfully.");
    }
}
