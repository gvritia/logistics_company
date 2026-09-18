package com.lcorp.console.exception;

public abstract class LogisticsException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    protected LogisticsException(String message) {
        super(message);
    }

    protected LogisticsException(String message, Throwable cause) {
        super(message, cause);
    }
}
