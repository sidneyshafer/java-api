package com.api.exception;

/**
 * Exception thrown when a requested resource is not found.
 */
public class ResourceNotFoundException extends RuntimeException {
    
    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public ResourceNotFoundException(String resourceType, Long id) {
        super(String.format("%s not found with ID: %d", resourceType, id));
    }

    public ResourceNotFoundException(String resourceType, String field, String value) {
        super(String.format("%s not found with %s: %s", resourceType, field, value));
    }
}
