package com.skillsphere.dto.bookmark;

import com.skillsphere.domain.BookmarkTargetType;

import java.time.LocalDateTime;

public record BookmarkResponse(
        Long id,
        BookmarkTargetType targetType,
        Long targetId,
        String targetName,
        String targetImage,
        LocalDateTime createdAt
) { }
