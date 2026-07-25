package com.skillsphere.exception;

import com.skillsphere.domain.Project;
import org.junit.jupiter.api.Test;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.data.util.TypeInformation;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void returnsBadRequestForMalformedNumericPathVariable() {
        MethodArgumentTypeMismatchException exception = new MethodArgumentTypeMismatchException(
                "not-a-number",
                Long.class,
                "projectId",
                null,
                new NumberFormatException("Invalid number")
        );

        ResponseEntity<ApiError> response = handler.handleTypeMismatch(exception);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Parameter 'projectId' must be a valid number.", response.getBody().message());
    }

    @Test
    void returnsBadRequestForUnknownSortField() {
        PropertyReferenceException exception = new PropertyReferenceException(
                "notAField",
                TypeInformation.of(Project.class),
                List.of()
        );

        ResponseEntity<ApiError> response = handler.handleInvalidSort(exception);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Unknown sort field 'notAField'.", response.getBody().message());
    }
}
