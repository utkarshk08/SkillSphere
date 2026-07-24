package com.skillsphere.dto.roadmap;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RoadmapItemRequest(
        @NotBlank @Size(max = 150) String title,
        boolean completed
) { }
