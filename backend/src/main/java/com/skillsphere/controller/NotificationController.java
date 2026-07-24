package com.skillsphere.controller;

import com.skillsphere.domain.User;
import com.skillsphere.dto.common.MessageResponse;
import com.skillsphere.dto.notification.NotificationRequest;
import com.skillsphere.dto.notification.NotificationResponse;
import com.skillsphere.dto.notification.NotificationUpdateRequest;
import com.skillsphere.service.notification.NotificationService;
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

import java.util.Map;

/** Paginated notification inbox with full CRUD as requested. */
@RestController
@RequestMapping("/api/notifications")
@Tag(name = "Notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public Page<NotificationResponse> getMine(
            @AuthenticationPrincipal User currentUser,
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable
    ) {
        return notificationService.getMine(currentUser, pageable);
    }

    @GetMapping("/unread-count")
    public Map<String, Long> unreadCount(@AuthenticationPrincipal User currentUser) {
        return Map.of("unreadCount", notificationService.unreadCount(currentUser));
    }

    @GetMapping("/{notificationId}")
    public NotificationResponse getById(@PathVariable Long notificationId, @AuthenticationPrincipal User currentUser) {
        return notificationService.getById(notificationId, currentUser);
    }

    @PostMapping
    public NotificationResponse create(
            @Valid @RequestBody NotificationRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        return notificationService.createPersonal(request, currentUser);
    }

    @PutMapping("/{notificationId}")
    public NotificationResponse update(
            @PathVariable Long notificationId,
            @Valid @RequestBody NotificationUpdateRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        return notificationService.update(notificationId, request, currentUser);
    }

    @DeleteMapping("/{notificationId}")
    public MessageResponse delete(@PathVariable Long notificationId, @AuthenticationPrincipal User currentUser) {
        notificationService.delete(notificationId, currentUser);
        return new MessageResponse("Notification deleted successfully.");
    }
}
