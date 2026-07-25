package com.skillsphere.dto.collaboration;

import com.skillsphere.domain.CollaborationRequestStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CollaborationRequestStatusUpdateRequest(
        @NotNull CollaborationRequestStatus status,
        @Size(max = 1000) String responseMessage
) { }
