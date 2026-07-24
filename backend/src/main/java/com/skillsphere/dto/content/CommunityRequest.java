package com.skillsphere.dto.content;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Set;

/**
 * Admin input for community creation and editing. Community resources are URL
 * strings by design, which is enough for a resource directory without extra upload
 * or resource entities.
 */
public record CommunityRequest(
        @NotBlank(message = "Community name is required")
        @Size(max = 150, message = "Community name must not exceed 150 characters")
        String name,

        @NotBlank(message = "Community description is required")
        @Size(max = 3000, message = "Community description must not exceed 3000 characters")
        String description,

        @Size(max = 50, message = "A community can have at most 50 resources")
        Set<
                @NotBlank(message = "Resource URL cannot be blank")
                @Pattern(regexp = "https?://.+", message = "Resource URL must start with http:// or https://")
                @Size(max = 500, message = "Resource URL must not exceed 500 characters")
                String> resources
) {
}
