package com.skillsphere.dto.profile;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Set;

/** Validation is kept at the API boundary so invalid profile data never reaches JPA. */
public record ProfileUpdateRequest(
        @NotBlank @Size(max = 60) String firstName,
        @NotBlank @Size(max = 60) String lastName,
        @NotBlank @Size(min = 3, max = 50) @Pattern(regexp = "^[A-Za-z0-9_.-]+$", message = "Username may contain letters, numbers, dots, underscores, and hyphens") String username,
        @NotBlank @Email @Size(max = 120) String email,
        @NotBlank @Size(max = 150) String collegeName,
        @NotBlank @Size(max = 100) String course,
        @NotBlank @Size(max = 50) String yearOfStudy,
        @NotBlank @Size(max = 80) String country,
        @Size(max = 1000) String bio,
        @Size(max = 255) @Pattern(regexp = "^(https?://.*)?$", message = "GitHub URL must start with http:// or https://") String githubUrl,
        @Size(max = 255) @Pattern(regexp = "^(https?://.*)?$", message = "LinkedIn URL must start with http:// or https://") String linkedinUrl,
        @Size(max = 255) @Pattern(regexp = "^(https?://.*)?$", message = "Portfolio URL must start with http:// or https://") String portfolioUrl,
        Set<@Size(max = 100) String> interests,
        @NotNull Boolean publicProfileVisibility
) { }
