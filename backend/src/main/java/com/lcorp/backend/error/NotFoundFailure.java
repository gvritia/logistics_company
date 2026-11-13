package com.lcorp.backend.error;

public class NotFoundFailure extends RuntimeException {
    public NotFoundFailure(String message) { super(message); }
}
