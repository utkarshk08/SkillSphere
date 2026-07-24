package com.skillsphere.dto.content;

import com.skillsphere.domain.DifficultyLevel;
import com.skillsphere.domain.ProjectStatus;

import java.time.LocalDate;
import java.util.Set;

/**
 * Full project view returned by project endpoints. The counts are calculated from
 * the members collection, so API consumers never need to calculate open positions.
 */
public record ProjectResponse(
        Long id,
        String title,
        String description,
        String githubLink,
        Set<String> projectImages,
        Set<String> techStack,
        Set<String> requiredSkills,
        LocalDate deadline,
        Integer maximumMembers,
        ProjectStatus status,
        DifficultyLevel difficultyLevel,
        Long ownerId,
        String ownerUsername,
        Long communityId,
        String communityName,
        int currentMemberCount,
        int openPositions,
        Set<UserSummaryResponse> members
) {
}
