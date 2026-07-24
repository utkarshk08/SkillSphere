package com.skillsphere.dto.content;

/**
 * A safe, small user representation for content responses. It deliberately omits
 * email, password, and private profile fields when a skill, project, or community
 * needs to identify a student.
 */
public record UserSummaryResponse(
        Long id,
        String username,
        String fullName,
        String profilePicturePath
) {
}
