package com.lcorp.console.exception;

// Скрывает технический SQLException от Service и консольного меню.
public class DataAccessException extends RuntimeException {

    private static final long serialVersionUID = 1L;
    // Throwable базовый класс для всех исключений
    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
