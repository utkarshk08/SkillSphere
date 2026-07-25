package com.skillsphere.dto.collaboration;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CollaborationRequestCreateRequest(
        Long receiverId,
        Long projectId,
        @NotBlank @Size(max = 1000) String message
) { }
