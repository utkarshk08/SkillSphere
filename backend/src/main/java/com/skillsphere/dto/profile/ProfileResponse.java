package com.skillsphere.dto.profile;

import com.skillsphere.domain.AuthProvider;
import com.skillsphere.domain.Role;

import java.util.List;
import java.util.Set;

/** Safe profile projection: password hashes never leave the backend. */
public record ProfileResponse(
        Long id,
        String username,
        String fullName,
        String firstName,
        String lastName,
        String email,
        String collegeName,
        String course,
        String yearOfStudy,
        String country,
        String bio,
        String profilePicturePath,
        String githubUrl,
        String linkedinUrl,
        String portfolioUrl,
        Set<String> interests,
        List<String> currentLearningSkills,
        List<String> teachingSkills,
        long projectsCount,
        boolean publicProfileVisibility,
        boolean verified,
        Role role,
        AuthProvider authProvider
) { }
