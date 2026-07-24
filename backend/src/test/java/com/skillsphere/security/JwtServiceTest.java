package com.skillsphere.security;

import com.skillsphere.domain.Role;
import com.skillsphere.domain.User;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pure unit tests keep JWT behavior testable without starting MySQL or the full Spring context.
 * This is a useful interview point: token generation/validation can be tested independently.
 */
class JwtServiceTest {

    private final String testSecret = Base64.getEncoder().encodeToString(
            "skillsphere-test-secret-must-be-at-least-thirty-two-bytes".getBytes(StandardCharsets.UTF_8)
    );

    @Test
    void generatesAValidTokenForTheSameUser() {
        JwtService jwtService = new JwtService(testSecret, 1_800_000);
        User user = new User();
        user.setUsername("utkarsh_dev");
        user.setEmail("utkarsh@example.com");
        user.setRole(Role.ROLE_USER);

        String token = jwtService.generateToken(user);

        assertEquals("utkarsh_dev", jwtService.extractUsername(token));
        assertTrue(jwtService.isTokenValid(token, user));
    }
}
