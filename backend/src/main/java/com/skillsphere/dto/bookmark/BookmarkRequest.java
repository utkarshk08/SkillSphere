package com.skillsphere.dto.bookmark;

import com.skillsphere.domain.BookmarkTargetType;
import jakarta.validation.constraints.NotNull;

public record BookmarkRequest(
        @NotNull BookmarkTargetType targetType,
        @NotNull Long targetId
) { }
