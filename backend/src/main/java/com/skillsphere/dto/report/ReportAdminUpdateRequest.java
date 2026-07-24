package com.skillsphere.dto.report;

import com.skillsphere.domain.AdminReportAction;
import com.skillsphere.domain.ReportStatus;
import jakarta.validation.constraints.NotNull;

public record ReportAdminUpdateRequest(
        @NotNull ReportStatus status,
        @NotNull AdminReportAction adminAction
) { }
