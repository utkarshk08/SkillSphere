package com.skillsphere.controller;

import com.skillsphere.domain.User;
import com.skillsphere.dto.common.MessageResponse;
import com.skillsphere.dto.report.ReportCreateRequest;
import com.skillsphere.dto.report.ReportResponse;
import com.skillsphere.dto.report.ReportUpdateRequest;
import com.skillsphere.service.report.ReportService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Student report creation and management endpoints; admin review lives under /api/admin. */
@RestController
@RequestMapping("/api/reports")
@Tag(name = "Reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping
    public Page<ReportResponse> getMine(
            @AuthenticationPrincipal User currentUser,
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable
    ) {
        return reportService.getMine(currentUser, pageable);
    }

    @GetMapping("/{reportId}")
    public ReportResponse getById(@PathVariable Long reportId, @AuthenticationPrincipal User currentUser) {
        return reportService.getById(reportId, currentUser);
    }

    @PostMapping
    public ReportResponse create(@Valid @RequestBody ReportCreateRequest request, @AuthenticationPrincipal User currentUser) {
        return reportService.create(request, currentUser);
    }

    @PutMapping("/{reportId}")
    public ReportResponse update(
            @PathVariable Long reportId,
            @Valid @RequestBody ReportUpdateRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        return reportService.update(reportId, request, currentUser);
    }

    @DeleteMapping("/{reportId}")
    public MessageResponse delete(@PathVariable Long reportId, @AuthenticationPrincipal User currentUser) {
        reportService.delete(reportId, currentUser);
        return new MessageResponse("Report deleted successfully.");
    }
}
