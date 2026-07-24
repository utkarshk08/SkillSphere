package com.skillsphere.dto.notification;

import com.skillsphere.domain.NotificationType;

import java.time.LocalDateTime;

public record NotificationResponse(
        Long id,
        NotificationType type,
        String message,
        boolean read,
        LocalDateTime createdAt
) { }
