package com.lcorp.backend.security;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;




public final class CompatiblePasswordEncoder implements PasswordEncoder {
    private static final String PREFIX = "{sha256-bcrypt}";
    private final BCryptPasswordEncoder bcrypt;

    public CompatiblePasswordEncoder(int strength) {
        this.bcrypt = new BCryptPasswordEncoder(strength);
    }

    @Override
    public String encode(CharSequence rawPassword) {
        return PREFIX + bcrypt.encode(digest(rawPassword));
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        if (rawPassword == null || encodedPassword == null) return false;
        if (encodedPassword.startsWith(PREFIX)) {
            return bcrypt.matches(digest(rawPassword), encodedPassword.substring(PREFIX.length()));
        }
        return bcrypt.matches(rawPassword, encodedPassword);
    }

    @Override
    public boolean upgradeEncoding(String encodedPassword) {
        return !encodedPassword.startsWith(PREFIX)
            || bcrypt.upgradeEncoding(encodedPassword.substring(PREFIX.length()));
    }

    private static String digest(CharSequence password) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256")
                .digest(password.toString().getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(bytes);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 unavailable", ex);
        }
    }
}
