package com.skillsphere.exception;

/** Thrown for a valid JSON request that violates a simple business rule. */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
