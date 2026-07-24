package com.skillsphere.dto.content;

import com.skillsphere.domain.SkillIntent;
import com.skillsphere.domain.SkillLevel;

/**
 * Read model for a skill. Returning a DTO instead of the entity prevents JPA
 * relationships and unrelated account fields from leaking through the REST API.
 */
public record SkillResponse(
        Long id,
        String name,
        SkillLevel level,
        SkillIntent intent,
        String description,
        Integer experienceMonths,
        Long ownerId,
        String ownerUsername
) {
}
