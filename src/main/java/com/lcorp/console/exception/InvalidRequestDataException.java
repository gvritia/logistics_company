package com.lcorp.console.exception;

public class InvalidRequestDataException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String fieldName;

    private final String reason;

    public InvalidRequestDataException(String fieldName, String reason) {
        super("Некорректные данные заявки: поле '" + fieldName + "' — " + reason);
        this.fieldName = fieldName;
        this.reason = reason;
    }

    public String getFieldName() {
        return fieldName;
    }

    public String getReason() {
        return reason;
    }
}
