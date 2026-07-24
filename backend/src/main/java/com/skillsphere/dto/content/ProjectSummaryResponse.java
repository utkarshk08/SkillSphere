package com.skillsphere.dto.content;

import com.skillsphere.domain.ProjectStatus;

import java.time.LocalDate;
import java.util.Set;

/**
 * Lightweight project information used when a community lists its projects.
 */
public record ProjectSummaryResponse(
        Long id,
        String title,
        String description,
        Set<String> techStack,
        Set<String> requiredSkills,
        LocalDate deadline,
        ProjectStatus status,
        String ownerUsername,
        int currentMemberCount,
        int openPositions
) {
}
