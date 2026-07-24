package com.skillsphere.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Input accepted from the registration form.
 *
 * Validation lives at the API boundary so invalid input is rejected before a service changes the
 * database. confirmPassword is intentionally a request-only field; it is never saved to User.
 */
@Schema(description = "Details required to create a local SkillSphere account")
public record RegistrationRequest(
        @NotBlank(message = "First name is required.")
        @Size(max = 60, message = "First name must be at most 60 characters.")
        @Schema(example = "Utkarsh")
        String firstName,

        @NotBlank(message = "Last name is required.")
        @Size(max = 60, message = "Last name must be at most 60 characters.")
        @Schema(example = "Khandelwal")
        String lastName,

        @NotBlank(message = "Username is required.")
        @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters.")
        @Pattern(regexp = "^[A-Za-z0-9_]+$", message = "Username may contain only letters, numbers, and underscores.")
        @Schema(example = "utkarsh_dev")
        String username,

        @NotBlank(message = "Email is required.")
        @Email(message = "Email must be valid.")
        @Size(max = 120, message = "Email must be at most 120 characters.")
        @Schema(example = "utkarsh@example.com")
        String email,

        @NotBlank(message = "Password is required.")
        @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters.")
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).+$",
                message = "Password must include uppercase, lowercase, number, and special character."
        )
        @Schema(example = "Utkarsh@123", format = "password")
        String password,

        @NotBlank(message = "Confirm password is required.")
        @Schema(example = "Utkarsh@123", format = "password")
        String confirmPassword,

        @NotBlank(message = "College name is required.")
        @Size(max = 150, message = "College name must be at most 150 characters.")
        @Schema(example = "Example Institute of Technology")
        String collegeName,

        @NotBlank(message = "Course is required.")
        @Size(max = 100, message = "Course must be at most 100 characters.")
        @Schema(example = "B.Tech Computer Science")
        String course,

        @NotBlank(message = "Year of study is required.")
        @Size(max = 50, message = "Year of study must be at most 50 characters.")
        @Schema(example = "3rd Year")
        String yearOfStudy,

        @NotBlank(message = "Country is required.")
        @Size(max = 80, message = "Country must be at most 80 characters.")
        @Schema(example = "India")
        String country,

        @Size(max = 1000, message = "Bio must be at most 1000 characters.")
        @Schema(example = "Learning Spring Boot and building practical projects.")
        String bio
) {
}
