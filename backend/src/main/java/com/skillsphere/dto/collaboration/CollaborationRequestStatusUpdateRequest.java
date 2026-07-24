package com.skillsphere.dto.collaboration;

import com.skillsphere.domain.CollaborationRequestStatus;
import jakarta.validation.constraints.NotNull;

public record CollaborationRequestStatusUpdateRequest(@NotNull CollaborationRequestStatus status) { }
