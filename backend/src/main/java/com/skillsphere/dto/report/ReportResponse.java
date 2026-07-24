package com.skillsphere.dto.report;

import com.skillsphere.domain.AdminReportAction;
import com.skillsphere.domain.ReportReason;
import com.skillsphere.domain.ReportStatus;
import com.skillsphere.domain.ReportedContentType;

import java.time.LocalDateTime;

public record ReportResponse(
        Long id,
        Long reporterId,
        String reporterUsername,
        Long reportedUserId,
        String reportedUsername,
        ReportedContentType reportedContentType,
        Long reportedContentId,
        ReportReason reason,
        String description,
        ReportStatus status,
        AdminReportAction adminAction,
        LocalDateTime createdAt
) { }
