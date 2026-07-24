package com.skillsphere.dto.report;

import com.skillsphere.domain.ReportReason;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReportUpdateRequest(
        @NotNull ReportReason reason,
        @Size(max = 1500) String description
) { }
