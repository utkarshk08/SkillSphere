package com.skillsphere.dto.content;

import com.skillsphere.domain.SkillIntent;
import com.skillsphere.domain.SkillLevel;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Input accepted when a signed-in student creates or edits one personal skill.
 * Bean Validation rejects invalid client data at the controller boundary before it
 * reaches the service or database.
 */
public record SkillRequest(
        @NotBlank(message = "Skill name is required")
        @Size(max = 100, message = "Skill name must not exceed 100 characters")
        String name,

        @NotNull(message = "Skill level is required")
        SkillLevel level,

        @NotNull(message = "Skill intent is required")
        SkillIntent intent,

        @NotBlank(message = "Skill description is required")
        @Size(max = 1000, message = "Skill description must not exceed 1000 characters")
        String description,

        @NotNull(message = "Experience in months is required")
        @Min(value = 0, message = "Experience in months cannot be negative")
        @Max(value = 600, message = "Experience in months must not exceed 600")
        Integer experienceMonths
) {
}
