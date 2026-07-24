package com.skillsphere.dto.collaboration;

import com.skillsphere.domain.CollaborationRequestStatus;

import java.time.LocalDateTime;

public record CollaborationRequestResponse(
        Long id,
        Long senderId,
        String senderUsername,
        String senderFullName,
        Long receiverId,
        String receiverUsername,
        String receiverFullName,
        String message,
        CollaborationRequestStatus status,
        LocalDateTime createdAt
) { }
