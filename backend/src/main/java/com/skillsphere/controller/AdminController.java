package com.skillsphere.controller;

import com.skillsphere.domain.ReportStatus;
import com.skillsphere.domain.ReportedContentType;
import com.skillsphere.domain.User;
import com.skillsphere.dto.admin.AdminUserResponse;
import com.skillsphere.dto.announcement.AnnouncementRequest;
import com.skillsphere.dto.announcement.AnnouncementResponse;
import com.skillsphere.dto.common.MessageResponse;
import com.skillsphere.dto.profile.ProfileResponse;
import com.skillsphere.dto.report.ReportAdminUpdateRequest;
import com.skillsphere.dto.report.ReportResponse;
import com.skillsphere.service.admin.AdminService;
import com.skillsphere.service.announcement.AnnouncementService;
import com.skillsphere.service.report.ReportService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Role-protected administration endpoints. @PreAuthorize makes the authorization rule
 * visible at the controller boundary while services retain business logic.
 */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Administration")
public class AdminController {

    private final AdminService adminService;
    private final ReportService reportService;
    private final AnnouncementService announcementService;

    public AdminController(
            AdminService adminService,
            ReportService reportService,
            AnnouncementService announcementService
    ) {
        this.adminService = adminService;
        this.reportService = reportService;
        this.announcementService = announcementService;
    }

    @GetMapping("/users")
    public Page<AdminUserResponse> getUsers(
            @RequestParam(required = false) String search,
            @PageableDefault(size = 10, sort = "username") Pageable pageable
    ) {
        return adminService.getUsers(search, pageable);
    }

    @PutMapping("/users/{userId}/verify")
    public ProfileResponse verifyProfile(@PathVariable Long userId) {
        return adminService.verifyProfile(userId);
    }

    @DeleteMapping("/users/{userId}")
    public MessageResponse deleteUser(@PathVariable Long userId, @AuthenticationPrincipal User currentAdmin) {
        adminService.deleteUser(userId, currentAdmin);
        return new MessageResponse("User deleted successfully.");
    }

    @DeleteMapping("/content/{contentType}/{contentId}")
    public MessageResponse deleteContent(
            @PathVariable ReportedContentType contentType,
            @PathVariable Long contentId,
            @AuthenticationPrincipal User currentAdmin
    ) {
        adminService.deleteContent(contentType, contentId, currentAdmin);
        return new MessageResponse("Reported content deleted successfully.");
    }

    @GetMapping("/reports")
    public Page<ReportResponse> getReports(
            @RequestParam(required = false) ReportStatus status,
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable
    ) {
        return reportService.getAll(status, pageable);
    }

    @PutMapping("/reports/{reportId}")
    public ReportResponse updateReport(
            @PathVariable Long reportId,
            @Valid @RequestBody ReportAdminUpdateRequest request
    ) {
        return reportService.updateByAdmin(reportId, request);
    }

    @DeleteMapping("/reports/{reportId}")
    public MessageResponse deleteReport(@PathVariable Long reportId, @AuthenticationPrincipal User currentAdmin) {
        reportService.delete(reportId, currentAdmin);
        return new MessageResponse("Report deleted successfully.");
    }

    @GetMapping("/announcements")
    public Page<AnnouncementResponse> getAnnouncements(@PageableDefault(size = 10, sort = "createdAt") Pageable pageable) {
        return announcementService.getAll(pageable);
    }

    @PostMapping("/announcements")
    public AnnouncementResponse createAnnouncement(
            @Valid @RequestBody AnnouncementRequest request,
            @AuthenticationPrincipal User currentAdmin
    ) {
        return announcementService.create(request, currentAdmin);
    }

    @PutMapping("/announcements/{announcementId}")
    public AnnouncementResponse updateAnnouncement(
            @PathVariable Long announcementId,
            @Valid @RequestBody AnnouncementRequest request
    ) {
        return announcementService.update(announcementId, request);
    }

    @DeleteMapping("/announcements/{announcementId}")
    public MessageResponse deleteAnnouncement(@PathVariable Long announcementId) {
        announcementService.delete(announcementId);
        return new MessageResponse("Announcement deleted successfully.");
    }
}
