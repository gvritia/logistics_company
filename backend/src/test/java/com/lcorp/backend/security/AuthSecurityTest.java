package com.lcorp.backend.security;

import com.lcorp.backend.domain.Role;
import com.lcorp.backend.domain.UserAccount;
import com.lcorp.backend.error.RateLimitFailure;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Base64;
import java.util.Date;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

class AuthSecurityTest {
    @Test
    void rejectsTamperedTokensAndWrongIssuer() {
        String secret = Base64.getEncoder().encodeToString(new byte[32]);
        JwtService jwt = new JwtService(secret, "test-issuer", 15, 14);
        UserAccount user = new UserAccount(UUID.randomUUID(), "test@example.org", "Test", "hash", Set.of(Role.ADMIN));
        String token = jwt.issue(user).value();

        assertEquals(user.getId(), jwt.parse(token).userId());
        assertEquals(0, jwt.parse(token).authVersion());
        String[] parts = token.split("\\.");
        String changedSignature = (parts[2].charAt(0) == 'A' ? "B" : "A") + parts[2].substring(1);
        assertThrows(JwtException.class, () -> jwt.parse(parts[0] + "." + parts[1] + "." + changedSignature));
        assertThrows(JwtException.class, () -> new JwtService(secret, "other-issuer", 15, 14).parse(token));

        var key = Keys.hmacShaKeyFor(Base64.getDecoder().decode(secret));
        String expired = Jwts.builder().issuer("test-issuer").subject(user.getId().toString())
            .claim("typ", "access").claim("av", 0).expiration(new Date(0))
            .signWith(key, Jwts.SIG.HS256).compact();
        assertThrows(JwtException.class, () -> jwt.parse(expired));
        String wrongType = Jwts.builder().issuer("test-issuer").subject(user.getId().toString())
            .claim("typ", "refresh").claim("av", 0).expiration(new Date(System.currentTimeMillis() + 60_000))
            .signWith(key, Jwts.SIG.HS256).compact();
        assertThrows(JwtException.class, () -> jwt.parse(wrongType));
    }

    @Test
    void acceptsPasswordsFromTwelveToSeventyTwoCharacters() {
        PasswordPolicy policy = new PasswordPolicy();
        policy.validate("0123456789ab");
        policy.validate("a".repeat(72));
        policy.validate("я".repeat(72));
        policy.validate("🔒".repeat(72));
        assertThrows(IllegalArgumentException.class, () -> policy.validate("short"));
        assertThrows(IllegalArgumentException.class, () -> policy.validate("🔒".repeat(11)));
        assertThrows(IllegalArgumentException.class, () -> policy.validate(" "));
        assertThrows(IllegalArgumentException.class, () -> policy.validate("a".repeat(73)));
        assertThrows(IllegalArgumentException.class, () -> policy.validate("я".repeat(73)));
        assertThrows(IllegalArgumentException.class, () -> policy.validate("🔒".repeat(73)));
    }

    @Test
    void verifiesLegacyHashesAndFullUnicodePasswords() {
        var encoder = new CompatiblePasswordEncoder(4);
        String legacy = new BCryptPasswordEncoder(4).encode("old-password-123");
        assertTrue(encoder.matches("old-password-123", legacy));
        String password = "я".repeat(72);
        String encoded = encoder.encode(password);
        assertTrue(encoder.matches(password, encoded));
        assertFalse(encoder.matches("я".repeat(71) + "ю", encoded));
    }

    @Test
    void limitsAttemptsPerRemoteAddress() {
        LoginRateLimiter limiter = new LoginRateLimiter();
        for (int i = 0; i < 20; i++) limiter.check("192.0.2.10");
        assertThrows(RateLimitFailure.class, () -> limiter.check("192.0.2.10"));
        limiter.check("192.0.2.11");
        RegistrationRateLimiter registrations = new RegistrationRateLimiter();
        for (int i = 0; i < 5; i++) registrations.check("192.0.2.10");
        assertThrows(RateLimitFailure.class, () -> registrations.check("192.0.2.10"));
        registrations.check("192.0.2.11");
    }
}
