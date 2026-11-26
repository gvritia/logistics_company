package com.lcorp.backend.error;

public class ConflictFailure extends RuntimeException {
    public ConflictFailure(String message) {
        super(message);
    }
}
