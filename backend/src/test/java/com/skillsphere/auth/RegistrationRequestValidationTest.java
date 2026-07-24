package com.skillsphere.auth;

import com.skillsphere.auth.dto.RegistrationRequest;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests the stated password policy at the DTO boundary, before persistence is involved. */
class RegistrationRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void acceptsTheRequiredPasswordFormat() {
        RegistrationRequest request = requestWithPassword("Utkarsh@123");
        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void rejectsPasswordWithoutUppercaseAndSpecialCharacter() {
        RegistrationRequest request = requestWithPassword("password123");
        assertFalse(validator.validate(request).isEmpty());
    }

    private RegistrationRequest requestWithPassword(String password) {
        return new RegistrationRequest(
                "Utkarsh", "Khandelwal", "utkarsh_dev", "utkarsh@example.com", password, password,
                "Example College", "B.Tech", "3rd Year", "India", null
        );
    }
}
