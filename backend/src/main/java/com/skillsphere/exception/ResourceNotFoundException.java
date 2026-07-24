package com.skillsphere.exception;

/** Thrown by a service when the requested database resource does not exist. */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
