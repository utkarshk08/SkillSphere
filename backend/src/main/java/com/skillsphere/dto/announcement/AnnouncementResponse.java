package com.skillsphere.dto.announcement;

import java.time.LocalDateTime;

public record AnnouncementResponse(
        Long id,
        String title,
        String message,
        boolean active,
        Long createdById,
        String createdByUsername,
        LocalDateTime createdAt
) { }
