package com.api.exception;

/**
 * Exception thrown when optimistic locking fails due to concurrent modification.
 */
public class OptimisticLockException extends RuntimeException {
    
    public OptimisticLockException(String message) {
        super(message);
    }

    public OptimisticLockException(String message, Throwable cause) {
        super(message, cause);
    }
}
