package com.lcorp.console.util;

public class InputClosedException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public InputClosedException(String message, Throwable cause) {
        super(message, cause);
    }
}
