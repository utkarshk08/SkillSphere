package com.skillsphere.dto.collaboration;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CollaborationRequestCreateRequest(
        @NotNull Long receiverId,
        @NotBlank @Size(max = 1000) String message
) { }
