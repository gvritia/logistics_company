package com.lcorp.backend.security;

import org.springframework.stereotype.Component;

@Component
public class PasswordPolicy {
    public void validate(String password) {
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Пароль должен содержать от 12 до 72 символов");
        }
        int length = password.codePointCount(0, password.length());
        if (length < 12 || length > 72) {
            throw new IllegalArgumentException("Пароль должен содержать от 12 до 72 символов");
        }
    }
}
