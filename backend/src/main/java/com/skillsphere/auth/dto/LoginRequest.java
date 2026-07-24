package com.skillsphere.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Request body for the email-and-password login endpoint. */
public record LoginRequest(
        @NotBlank(message = "Email is required.")
        @Email(message = "Email must be valid.")
        @Schema(example = "utkarsh@example.com")
        String email,

        @NotBlank(message = "Password is required.")
        @Size(max = 100, message = "Password must be at most 100 characters.")
        @Schema(example = "Utkarsh@123", format = "password")
        String password
) {
}
