package com.skillsphere.exception;

/** Thrown when a request has no valid authenticated user for the requested action. */
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }
}
