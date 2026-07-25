package com.skillsphere.dto.content;

import java.util.Set;

/**
 * Community view that exposes useful totals while keeping potentially large member
 * and project lists on their own paginated endpoints.
 */
public record CommunityResponse(
        Long id,
        String name,
        String description,
        Set<String> resources,
        int memberCount,
        int projectCount,
        int resourceCount,
        boolean member
) {
}
