package com.lcorp.backend.security;

import org.springframework.stereotype.Component;

@Component
public class PasswordPolicy {
    public void validate(String password) {
        if (password == null || password.isBlank() || password.codePointCount(0, password.length()) > 72) {
            throw new IllegalArgumentException("Пароль должен содержать от 1 до 72 символов");
        }
    }
}
