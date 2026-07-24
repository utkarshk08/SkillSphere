package com.skillsphere.exception;

/** Thrown by file-upload services when an uploaded file cannot be accepted or stored safely. */
public class FileUploadException extends RuntimeException {

    public FileUploadException(String message) {
        super(message);
    }

    public FileUploadException(String message, Throwable cause) {
        super(message, cause);
    }
}
