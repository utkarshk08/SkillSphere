package com.skillsphere.dto.notification;

import com.skillsphere.domain.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record NotificationRequest(
        @NotBlank @Size(max = 500) String message,
        @NotNull NotificationType type
) { }
