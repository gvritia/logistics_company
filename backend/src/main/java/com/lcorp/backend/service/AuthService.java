package com.lcorp.backend.service;

import com.lcorp.backend.domain.AuditEvent;
import com.lcorp.backend.domain.RefreshSession;
import com.lcorp.backend.domain.UserAccount;
import com.lcorp.backend.dto.AuthDtos.AuthResponse;
import com.lcorp.backend.dto.AuthDtos.CurrentUser;
import com.lcorp.backend.error.AuthFailure;
import com.lcorp.backend.repository.AuditEventRepository;
import com.lcorp.backend.repository.RefreshSessionRepository;
import com.lcorp.backend.repository.UserAccountRepository;
import com.lcorp.backend.security.JwtService;
import com.lcorp.backend.security.PasswordPolicy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class AuthService {
    private final UserAccountRepository users;
    private final RefreshSessionRepository sessions;
    private final AuditEventRepository audit;
    private final PasswordEncoder encoder;
    private final PasswordPolicy passwordPolicy;
    private final JwtService jwt;
    private final String dummyHash;
    private final int maxFailedAttempts;
    private final int lockMinutes;
    private final SecureRandom random = new SecureRandom();

    public AuthService(UserAccountRepository users, RefreshSessionRepository sessions,
                       AuditEventRepository audit, PasswordEncoder encoder,
                       PasswordPolicy passwordPolicy, JwtService jwt,
                       @Value("${app.auth.max-failed-attempts}") int maxFailedAttempts,
                       @Value("${app.auth.lock-minutes}") int lockMinutes) {
        this.users = users;
        this.sessions = sessions;
        this.audit = audit;
        this.encoder = encoder;
        this.passwordPolicy = passwordPolicy;
        this.jwt = jwt;
        this.dummyHash = encoder.encode(UUID.randomUUID().toString());
        this.maxFailedAttempts = maxFailedAttempts;
        this.lockMinutes = lockMinutes;
        if (maxFailedAttempts < 1 || lockMinutes < 1) throw new IllegalStateException("Invalid login limits");
    }

    @Transactional(noRollbackFor = AuthFailure.class)
    public AuthResponse login(String email, String password, String correlationId) {
        UserAccount user = users.findForLogin(email.trim().toLowerCase(Locale.ROOT)).orElse(null);
        boolean matches = encoder.matches(password, user == null ? dummyHash : user.getPasswordHash());
        Instant now = Instant.now();
        boolean locked = user != null && user.getLockedUntil() != null && user.getLockedUntil().isAfter(now);
        if (user != null && user.getLockedUntil() != null && !locked) {
            user.setFailedLoginAttempts(0);
            user.setLockedUntil(null);
        }
        if (user == null || !user.isEnabled() || locked || !matches) {
            if (user != null && user.isEnabled() && !locked) {
                int count = user.getFailedLoginAttempts() + 1;
                user.setFailedLoginAttempts(count);
                if (count >= maxFailedAttempts) user.setLockedUntil(now.plus(lockMinutes, ChronoUnit.MINUTES));
            }
            record(user, "LOGIN_FAILED", "Неудачная попытка входа", correlationId);
            throw new AuthFailure();
        }
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        record(user, "LOGIN_SUCCESS", "Успешный вход", correlationId);
        return issue(user, now);
    }

    @Transactional(noRollbackFor = AuthFailure.class)
    public AuthResponse refresh(String refreshToken, String correlationId) {
        RefreshSession old = sessions.findLockedByHash(hash(refreshToken)).orElseThrow(AuthFailure::new);
        Instant now = Instant.now();
        UserAccount user = old.getUser();
        if (old.getRevokedAt() != null) {
            sessions.revokeActiveByUser(user.getId(), now);
            record(user, "REFRESH_REUSE", "Повторное использование refresh-токена", correlationId);
            throw new AuthFailure();
        }
        if (!user.isEnabled() || !old.getExpiresAt().isAfter(now)) throw new AuthFailure();
        old.setRevokedAt(now);
        return issue(user, now);
    }

    @Transactional
    public void logout(String refreshToken, String correlationId) {
        sessions.findLockedByHash(hash(refreshToken)).ifPresent(session -> {
            if (session.getRevokedAt() == null) {
                session.setRevokedAt(Instant.now());
                record(session.getUser(), "LOGOUT", "Выход из системы", correlationId);
            }
        });
    }

    @Transactional(readOnly = true)
    public CurrentUser me(UUID userId) {
        return current(users.findById(userId).filter(UserAccount::isEnabled).orElseThrow(AuthFailure::new));
    }

    @Transactional
    public void changePassword(UUID userId, String currentPassword, String newPassword, String correlationId) {
        UserAccount user = users.findLocked(userId).filter(UserAccount::isEnabled).orElseThrow(AuthFailure::new);
        if (!encoder.matches(currentPassword, user.getPasswordHash())) throw new AuthFailure();
        passwordPolicy.validate(newPassword);
        if (encoder.matches(newPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("Новый пароль должен отличаться от текущего");
        }
        user.setPasswordHash(encoder.encode(newPassword));
        user.incrementAuthVersion();
        user.setPasswordChangeRequired(false);
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        sessions.revokeActiveByUser(userId, Instant.now());
        record(user, "PASSWORD_CHANGED", "Пароль изменён", correlationId);
    }

    private AuthResponse issue(UserAccount user, Instant now) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String refresh = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        sessions.save(new RefreshSession(UUID.randomUUID(), user, hash(refresh), now,
            now.plus(jwt.refreshDays(), ChronoUnit.DAYS)));
        JwtService.IssuedToken access = jwt.issue(user);
        return new AuthResponse(access.value(), refresh, access.expiresAt(), current(user));
    }

    private static CurrentUser current(UserAccount user) {
        return new CurrentUser(user.getId(), user.getEmail(), user.getDisplayName(),
            Set.copyOf(user.getRoles()), user.isPasswordChangeRequired());
    }

    private void record(UserAccount user, String type, String description, String correlationId) {
        audit.save(new AuditEvent(UUID.randomUUID(), user, type, "USER_ACCOUNT",
            user == null ? null : user.getId(), Instant.now(), description, correlationId));
    }

    private static String hash(String token) {
        if (token == null || !token.matches("[A-Za-z0-9_-]{43}")) throw new AuthFailure();
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.US_ASCII));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 unavailable", ex);
        }
    }
}
