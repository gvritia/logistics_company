package com.lcorp.backend.security;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
public class PasswordPolicy {
    public void validate(String password) {
        if (password == null || password.length() < 12 || password.isBlank()
            || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("Пароль должен содержать не менее 12 и не более 72 байт UTF-8");
        }
    }
}
