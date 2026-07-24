package com.skillsphere.service.notification;

import com.skillsphere.domain.Notification;
import com.skillsphere.domain.NotificationType;
import com.skillsphere.domain.Role;
import com.skillsphere.domain.User;
import com.skillsphere.dto.notification.NotificationRequest;
import com.skillsphere.dto.notification.NotificationResponse;
import com.skillsphere.dto.notification.NotificationUpdateRequest;
import com.skillsphere.exception.ResourceNotFoundException;
import com.skillsphere.exception.UnauthorizedException;
import com.skillsphere.repository.NotificationRepository;
import com.skillsphere.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Persists in-app notifications and exposes ordinary REST pagination instead of a
 * WebSocket layer. Other services use createForUser for simple system notifications.
 */
@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationService(NotificationRepository notificationRepository, UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponse> getMine(User currentUser, Pageable pageable) {
        return notificationRepository.findByRecipientId(currentUser.getId(), pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public long unreadCount(User currentUser) {
        return notificationRepository.countByRecipientIdAndReadFalse(currentUser.getId());
    }

    @Transactional(readOnly = true)
    public NotificationResponse getById(Long notificationId, User currentUser) {
        Notification notification = findNotification(notificationId);
        requireRecipientOrAdmin(notification, currentUser);
        return toResponse(notification);
    }

    @Transactional
    public NotificationResponse createPersonal(NotificationRequest request, User currentUser) {
        return toResponse(createForUser(currentUser, request.type(), request.message()));
    }

    @Transactional
    public NotificationResponse update(Long notificationId, NotificationUpdateRequest request, User currentUser) {
        Notification notification = findNotification(notificationId);
        requireRecipientOrAdmin(notification, currentUser);
        notification.setMessage(request.message().trim());
        notification.setType(request.type());
        notification.setRead(request.read());
        return toResponse(notificationRepository.save(notification));
    }

    @Transactional
    public void delete(Long notificationId, User currentUser) {
        Notification notification = findNotification(notificationId);
        requireRecipientOrAdmin(notification, currentUser);
        notificationRepository.delete(notification);
    }

    @Transactional
    public Notification createForUser(User recipient, NotificationType type, String message) {
        Notification notification = new Notification();
        notification.setRecipient(recipient);
        notification.setType(type);
        notification.setMessage(message.trim());
        return notificationRepository.save(notification);
    }

    @Transactional
    public void notifyAllUsers(NotificationType type, String message) {
        // A single loop is sufficient for this student project and avoids introducing queues.
        userRepository.findAll().forEach(user -> createForUser(user, type, message));
    }

    private Notification findNotification(Long notificationId) {
        return notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found: " + notificationId));
    }

    private void requireRecipientOrAdmin(Notification notification, User currentUser) {
        if (!notification.getRecipient().getId().equals(currentUser.getId()) && currentUser.getRole() != Role.ROLE_ADMIN) {
            throw new UnauthorizedException("You cannot manage this notification.");
        }
    }

    private NotificationResponse toResponse(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getType(),
                notification.getMessage(),
                notification.isRead(),
                notification.getCreatedAt()
        );
    }
}
