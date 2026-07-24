package com.skillsphere.dto.content;

import com.skillsphere.domain.DifficultyLevel;
import com.skillsphere.domain.ProjectStatus;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.Set;

/**
 * Validated request body for project creation and updates. Simple string sets are
 * used for tags, which keeps the API focused on the project rather than creating
 * extra catalogue entities. Images are uploaded through the dedicated multipart endpoint.
 */
public record ProjectRequest(
        @NotBlank(message = "Project title is required")
        @Size(max = 150, message = "Project title must not exceed 150 characters")
        String title,

        @NotBlank(message = "Project description is required")
        @Size(max = 3000, message = "Project description must not exceed 3000 characters")
        String description,

        @Pattern(regexp = "^(https?://.*)?$", message = "GitHub link must start with http:// or https://")
        @Size(max = 500, message = "GitHub link must not exceed 500 characters")
        String githubLink,

        @NotEmpty(message = "At least one technology is required")
        @Size(max = 20, message = "A project can have at most 20 technologies")
        Set<
                @NotBlank(message = "Technology cannot be blank")
                @Size(max = 100, message = "Technology must not exceed 100 characters")
                String> techStack,

        @NotEmpty(message = "At least one required skill is required")
        @Size(max = 20, message = "A project can have at most 20 required skills")
        Set<
                @NotBlank(message = "Required skill cannot be blank")
                @Size(max = 100, message = "Required skill must not exceed 100 characters")
                String> requiredSkills,

        @NotNull(message = "Project deadline is required")
        @FutureOrPresent(message = "Project deadline cannot be in the past")
        LocalDate deadline,

        @NotNull(message = "Maximum members is required")
        @Min(value = 1, message = "A project must allow at least one member")
        @Max(value = 20, message = "A project can have at most 20 members")
        Integer maximumMembers,

        @NotNull(message = "Project status is required")
        ProjectStatus status,

        @NotNull(message = "Difficulty level is required")
        DifficultyLevel difficultyLevel,

        Long communityId
) {
}
