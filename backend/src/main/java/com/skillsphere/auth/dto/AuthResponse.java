package com.skillsphere.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Returned after local registration or login. The frontend stores accessToken and attaches it as
 * Authorization: Bearer <token> on future requests. There are intentionally no refresh tokens.
 */
public record AuthResponse(
        @Schema(description = "Signed JWT access token", example = "eyJhbGciOiJIUzI1NiJ9...")
        String accessToken,
        @Schema(example = "Bearer")
        String tokenType,
        @Schema(description = "JWT lifetime in seconds", example = "1800")
        long expiresIn,
        UserResponse user
) {
}
