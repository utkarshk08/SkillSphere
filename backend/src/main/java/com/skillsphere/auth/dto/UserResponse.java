package com.skillsphere.auth.dto;

import com.skillsphere.domain.User;

/**
 * Safe user data returned to the frontend. Password hashes are never exposed in an API response.
 */
public record UserResponse(
        Long id,
        String firstName,
        String lastName,
        String fullName,
        String username,
        String email,
        String collegeName,
        String course,
        String yearOfStudy,
        String country,
        String bio,
        String profilePicturePath,
        boolean publicProfileVisibility,
        boolean verified,
        String role
) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getFullName(),
                user.getUsername(),
                user.getEmail(),
                user.getCollegeName(),
                user.getCourse(),
                user.getYearOfStudy(),
                user.getCountry(),
                user.getBio(),
                user.getProfilePicturePath(),
                user.isPublicProfileVisibility(),
                user.isVerified(),
                user.getRole().name()
        );
    }
}
