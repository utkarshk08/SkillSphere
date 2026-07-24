package com.skillsphere.service.report;

import com.skillsphere.domain.AdminReportAction;
import com.skillsphere.domain.Report;
import com.skillsphere.domain.ReportedContentType;
import com.skillsphere.domain.ReportStatus;
import com.skillsphere.domain.NotificationType;
import com.skillsphere.domain.Role;
import com.skillsphere.domain.User;
import com.skillsphere.dto.report.ReportAdminUpdateRequest;
import com.skillsphere.dto.report.ReportCreateRequest;
import com.skillsphere.dto.report.ReportResponse;
import com.skillsphere.dto.report.ReportUpdateRequest;
import com.skillsphere.exception.BadRequestException;
import com.skillsphere.exception.ResourceNotFoundException;
import com.skillsphere.exception.UnauthorizedException;
import com.skillsphere.repository.ReportRepository;
import com.skillsphere.repository.CommunityRepository;
import com.skillsphere.repository.ProjectRepository;
import com.skillsphere.repository.SkillRepository;
import com.skillsphere.repository.UserRepository;
import com.skillsphere.service.notification.NotificationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Keeps reports in a transparent PENDING_REVIEW/RESOLVED lifecycle. Complex moderation
 * workflows are intentionally avoided; admin actions are recorded on the report itself.
 */
@Service
public class ReportService {

    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final CommunityRepository communityRepository;
    private final SkillRepository skillRepository;
    private final NotificationService notificationService;

    public ReportService(
            ReportRepository reportRepository,
            UserRepository userRepository,
            ProjectRepository projectRepository,
            CommunityRepository communityRepository,
            SkillRepository skillRepository,
            NotificationService notificationService
    ) {
        this.reportRepository = reportRepository;
        this.userRepository = userRepository;
        this.projectRepository = projectRepository;
        this.communityRepository = communityRepository;
        this.skillRepository = skillRepository;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public Page<ReportResponse> getMine(User currentUser, Pageable pageable) {
        return reportRepository.findByReporterId(currentUser.getId(), pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public ReportResponse getById(Long reportId, User currentUser) {
        Report report = findReport(reportId);
        requireReporterOrAdmin(report, currentUser);
        return toResponse(report);
    }

    @Transactional(readOnly = true)
    public Page<ReportResponse> getAll(ReportStatus status, Pageable pageable) {
        return status == null
                ? reportRepository.findAll(pageable).map(this::toResponse)
                : reportRepository.findByStatus(status, pageable).map(this::toResponse);
    }

    @Transactional
    public ReportResponse create(ReportCreateRequest request, User currentUser) {
        validateTarget(request);
        Report report = new Report();
        report.setReporter(currentUser);
        if (request.reportedUserId() != null) {
            if (currentUser.getId().equals(request.reportedUserId())) {
                throw new BadRequestException("You cannot report yourself.");
            }
            report.setReportedUser(userRepository.findById(request.reportedUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("Reported user not found: " + request.reportedUserId())));
        }
        report.setReportedContentType(request.reportedContentType());
        report.setReportedContentId(request.reportedContentId());
        if (request.reportedContentType() != null) {
            validateContentExists(request.reportedContentType(), request.reportedContentId(), currentUser);
        }
        report.setReason(request.reason());
        report.setDescription(blankToNull(request.description()));
        return toResponse(reportRepository.save(report));
    }

    @Transactional
    public ReportResponse update(Long reportId, ReportUpdateRequest request, User currentUser) {
        Report report = findReport(reportId);
        requireReporterOrAdmin(report, currentUser);
        if (report.getStatus() != ReportStatus.PENDING_REVIEW && currentUser.getRole() != Role.ROLE_ADMIN) {
            throw new BadRequestException("Resolved reports cannot be edited.");
        }
        report.setReason(request.reason());
        report.setDescription(blankToNull(request.description()));
        return toResponse(reportRepository.save(report));
    }

    @Transactional
    public void delete(Long reportId, User currentUser) {
        Report report = findReport(reportId);
        requireReporterOrAdmin(report, currentUser);
        reportRepository.delete(report);
    }

    @Transactional
    public ReportResponse updateByAdmin(Long reportId, ReportAdminUpdateRequest request) {
        Report report = findReport(reportId);
        report.setStatus(request.status());
        report.setAdminAction(request.adminAction());
        Report saved = reportRepository.save(report);
        if (request.adminAction() == AdminReportAction.WARNED_USER && saved.getReportedUser() != null) {
            notificationService.createForUser(
                    saved.getReportedUser(),
                    NotificationType.GENERAL,
                    "An administrator warned you after reviewing a report."
            );
        }
        return toResponse(saved);
    }

    /**
     * Retains moderation history when a reported object is removed, rather than leaving a
     * dangling generic content id in a PENDING report.
     */
    @Transactional
    public void resolveDeletedContent(ReportedContentType type, Long contentId) {
        reportRepository.findByReportedContentTypeAndReportedContentId(type, contentId).forEach(report -> {
            report.setStatus(ReportStatus.RESOLVED);
            report.setAdminAction(AdminReportAction.DELETED_CONTENT);
        });
    }

    private void validateTarget(ReportCreateRequest request) {
        boolean userTarget = request.reportedUserId() != null;
        boolean contentType = request.reportedContentType() != null;
        boolean contentId = request.reportedContentId() != null;
        if (contentType != contentId) {
            throw new BadRequestException("Content type and content id must be provided together.");
        }
        boolean contentTarget = contentType && contentId;
        if (userTarget == contentTarget) {
            throw new BadRequestException("Choose exactly one user or one content item to report.");
        }
    }

    private void validateContentExists(ReportedContentType type, Long contentId, User currentUser) {
        boolean exists = switch (type) {
            case PROFILE -> userRepository.existsById(contentId);
            case PROJECT -> projectRepository.existsById(contentId);
            case COMMUNITY -> communityRepository.existsById(contentId);
            case SKILL -> skillRepository.existsById(contentId);
        };
        if (!exists) {
            throw new ResourceNotFoundException("Reported content not found: " + contentId);
        }
        if (type == ReportedContentType.PROFILE && currentUser.getId().equals(contentId)) {
            throw new BadRequestException("You cannot report yourself.");
        }
    }

    private Report findReport(Long reportId) {
        return reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found: " + reportId));
    }

    private void requireReporterOrAdmin(Report report, User currentUser) {
        if (!report.getReporter().getId().equals(currentUser.getId()) && currentUser.getRole() != Role.ROLE_ADMIN) {
            throw new UnauthorizedException("You cannot manage this report.");
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private ReportResponse toResponse(Report report) {
        return new ReportResponse(
                report.getId(),
                report.getReporter().getId(),
                report.getReporter().getUsername(),
                report.getReportedUser() == null ? null : report.getReportedUser().getId(),
                report.getReportedUser() == null ? null : report.getReportedUser().getUsername(),
                report.getReportedContentType(),
                report.getReportedContentId(),
                report.getReason(),
                report.getDescription(),
                report.getStatus(),
                report.getAdminAction(),
                report.getCreatedAt()
        );
    }
}
