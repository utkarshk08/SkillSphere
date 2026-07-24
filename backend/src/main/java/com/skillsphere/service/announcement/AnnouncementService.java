package com.skillsphere.service.announcement;

import com.skillsphere.domain.Announcement;
import com.skillsphere.domain.NotificationType;
import com.skillsphere.domain.User;
import com.skillsphere.dto.announcement.AnnouncementRequest;
import com.skillsphere.dto.announcement.AnnouncementResponse;
import com.skillsphere.exception.ResourceNotFoundException;
import com.skillsphere.repository.AnnouncementRepository;
import com.skillsphere.service.notification.NotificationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Keeps administrator announcements separate from the notification inbox while still
 * notifying students when a new active announcement is published.
 */
@Service
public class AnnouncementService {

    private final AnnouncementRepository announcementRepository;
    private final NotificationService notificationService;

    public AnnouncementService(AnnouncementRepository announcementRepository, NotificationService notificationService) {
        this.announcementRepository = announcementRepository;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public Page<AnnouncementResponse> getActive(Pageable pageable) {
        return announcementRepository.findByActiveTrue(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<AnnouncementResponse> getAll(Pageable pageable) {
        return announcementRepository.findAll(pageable).map(this::toResponse);
    }

    @Transactional
    public AnnouncementResponse create(AnnouncementRequest request, User admin) {
        Announcement announcement = new Announcement();
        announcement.setCreatedBy(admin);
        applyRequest(announcement, request);
        Announcement saved = announcementRepository.save(announcement);
        if (saved.isActive()) {
            notificationService.notifyAllUsers(NotificationType.ADMIN_ANNOUNCEMENT, saved.getTitle() + ": " + saved.getMessage());
        }
        return toResponse(saved);
    }

    @Transactional
    public AnnouncementResponse update(Long announcementId, AnnouncementRequest request) {
        Announcement announcement = findAnnouncement(announcementId);
        applyRequest(announcement, request);
        return toResponse(announcementRepository.save(announcement));
    }

    @Transactional
    public void delete(Long announcementId) {
        announcementRepository.delete(findAnnouncement(announcementId));
    }

    private Announcement findAnnouncement(Long announcementId) {
        return announcementRepository.findById(announcementId)
                .orElseThrow(() -> new ResourceNotFoundException("Announcement not found: " + announcementId));
    }

    private void applyRequest(Announcement announcement, AnnouncementRequest request) {
        announcement.setTitle(request.title().trim());
        announcement.setMessage(request.message().trim());
        announcement.setActive(request.active());
    }

    private AnnouncementResponse toResponse(Announcement announcement) {
        return new AnnouncementResponse(
                announcement.getId(),
                announcement.getTitle(),
                announcement.getMessage(),
                announcement.isActive(),
                announcement.getCreatedBy().getId(),
                announcement.getCreatedBy().getUsername(),
                announcement.getCreatedAt()
        );
    }
}
