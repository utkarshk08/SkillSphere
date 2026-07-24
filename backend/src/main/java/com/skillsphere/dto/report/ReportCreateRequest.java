package com.skillsphere.dto.report;

import com.skillsphere.domain.ReportReason;
import com.skillsphere.domain.ReportedContentType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReportCreateRequest(
        Long reportedUserId,
        ReportedContentType reportedContentType,
        Long reportedContentId,
        @NotNull ReportReason reason,
        @Size(max = 1500) String description
) { }
