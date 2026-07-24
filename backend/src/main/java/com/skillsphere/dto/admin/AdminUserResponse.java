package com.skillsphere.dto.admin;

import com.skillsphere.domain.AuthProvider;
import com.skillsphere.domain.Role;

public record AdminUserResponse(
        Long id,
        String username,
        String email,
        String fullName,
        String collegeName,
        Role role,
        AuthProvider authProvider,
        boolean verified,
        boolean publicProfileVisibility
) { }
