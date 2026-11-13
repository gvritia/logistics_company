package com.lcorp.backend.error;

public class AuthFailure extends RuntimeException {
    public AuthFailure() {
        super("Неверные учётные данные или недействительная сессия");
    }
}
