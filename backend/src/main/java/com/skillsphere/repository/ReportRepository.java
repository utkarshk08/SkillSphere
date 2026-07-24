package com.skillsphere.repository;

import com.skillsphere.domain.Report;
import com.skillsphere.domain.ReportStatus;
import com.skillsphere.domain.ReportedContentType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReportRepository extends JpaRepository<Report, Long> {
    Page<Report> findByReporterId(Long reporterId, Pageable pageable);
    Page<Report> findByStatus(ReportStatus status, Pageable pageable);
    long deleteByReporterIdOrReportedUserId(Long reporterId, Long reportedUserId);
    List<Report> findByReportedContentTypeAndReportedContentId(ReportedContentType type, Long contentId);
}
