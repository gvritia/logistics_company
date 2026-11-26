package com.lcorp.backend.error;

public class AuthFailure extends RuntimeException {
    public AuthFailure() {
        this("Неверные учётные данные или недействительная сессия");
    }

    public AuthFailure(String message) {
        super(message);
    }
}
