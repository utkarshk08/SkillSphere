package com.skillsphere.controller;

import com.skillsphere.dto.announcement.AnnouncementResponse;
import com.skillsphere.service.announcement.AnnouncementService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Read-only active announcements for the student dashboard. */
@RestController
@RequestMapping("/api/announcements")
@Tag(name = "Announcements")
public class AnnouncementController {

    private final AnnouncementService announcementService;

    public AnnouncementController(AnnouncementService announcementService) {
        this.announcementService = announcementService;
    }

    @GetMapping
    public Page<AnnouncementResponse> getActive(@PageableDefault(size = 10, sort = "createdAt") Pageable pageable) {
        return announcementService.getActive(pageable);
    }
}
