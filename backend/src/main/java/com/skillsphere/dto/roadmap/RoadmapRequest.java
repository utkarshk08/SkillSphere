package com.skillsphere.dto.roadmap;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record RoadmapRequest(
        @NotBlank @Size(max = 150) String title,
        @NotNull Boolean publicVisible,
        @Valid @Size(max = 50) List<RoadmapItemRequest> items
) { }
