package com.skillsphere.repository;

import com.skillsphere.domain.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    Page<Notification> findByRecipientId(Long recipientId, Pageable pageable);
    long countByRecipientIdAndReadFalse(Long recipientId);
    long deleteByRecipientId(Long recipientId);
}
