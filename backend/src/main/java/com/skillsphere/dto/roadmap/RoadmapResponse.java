package com.skillsphere.dto.roadmap;

import java.time.LocalDateTime;
import java.util.List;

/** Completion is calculated from the persisted items instead of stored redundantly. */
public record RoadmapResponse(
        Long id,
        Long ownerId,
        String ownerUsername,
        String title,
        boolean publicVisible,
        List<RoadmapItemResponse> items,
        int completionPercentage,
        LocalDateTime createdAt
) { }
