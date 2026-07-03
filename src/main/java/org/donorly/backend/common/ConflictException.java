package org.donorly.backend.common;

public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
