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
        Long projectId,
        String projectTitle,
        String message,
        String responseMessage,
        CollaborationRequestStatus status,
        LocalDateTime createdAt
) { }
