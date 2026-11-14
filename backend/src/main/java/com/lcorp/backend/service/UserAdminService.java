package com.lcorp.backend.service;

import com.lcorp.backend.domain.AuditEvent;
import com.lcorp.backend.domain.Role;
import com.lcorp.backend.domain.UserAccount;
import com.lcorp.backend.dto.UserAdminDtos;
import com.lcorp.backend.error.ConflictFailure;
import com.lcorp.backend.error.NotFoundFailure;
import com.lcorp.backend.repository.AuditEventRepository;
import com.lcorp.backend.repository.RefreshSessionRepository;
import com.lcorp.backend.repository.UserAccountRepository;
import com.lcorp.backend.security.PasswordPolicy;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class UserAdminService {
    private static final Set<String> SORT_FIELDS = Set.of("email", "displayName", "enabled");

    private final UserAccountRepository users;
    private final RefreshSessionRepository sessions;
    private final AuditEventRepository audit;
    private final PasswordEncoder encoder;
    private final PasswordPolicy passwordPolicy;
    private final SecureRandom random = new SecureRandom();

    public UserAdminService(UserAccountRepository users, RefreshSessionRepository sessions,
                            AuditEventRepository audit, PasswordEncoder encoder,
                            PasswordPolicy passwordPolicy) {
        this.users = users;
        this.sessions = sessions;
        this.audit = audit;
        this.encoder = encoder;
        this.passwordPolicy = passwordPolicy;
    }

    @Transactional(readOnly = true)
    public UserAdminDtos.Page list(int page, int size, String sort) {
        if (page < 0 || page > 1_000_000 || size < 1 || size > 50) {
            throw new IllegalArgumentException("page должен быть от 0 до 1000000, size — от 1 до 50");
        }
        String[] parts = sort.split(",", -1);
        if (parts.length != 2 || !SORT_FIELDS.contains(parts[0])
            || !(parts[1].equalsIgnoreCase("asc") || parts[1].equalsIgnoreCase("desc"))) {
            throw new IllegalArgumentException("Недопустимая сортировка пользователей");
        }
        Sort order = Sort.by(Sort.Direction.fromString(parts[1]), parts[0]).and(Sort.by("id"));
        var result = users.findAll(PageRequest.of(page, size, order));
        return new UserAdminDtos.Page(result.stream().map(UserAdminService::view).toList(),
            page, size, result.getTotalElements(), result.getTotalPages());
    }

    @Transactional(readOnly = true)
    public UserAdminDtos.User get(UUID id) {
        return view(users.findById(id).orElseThrow(() -> new NotFoundFailure("Пользователь не найден")));
    }

    @Transactional
    public UserAdminDtos.TemporaryPassword create(UserAdminDtos.Create body, UUID actorId, String correlationId) {
        String email = email(body.email());
        if (users.existsByEmailIgnoreCase(email)) throw new ConflictFailure("Логин уже занят");
        String temporaryPassword = temporaryPassword();
        UserAccount user = new UserAccount(UUID.randomUUID(), email, body.displayName().trim(),
            encoder.encode(temporaryPassword), Set.copyOf(body.roles()));
        users.saveAndFlush(user);
        record(actorId, "USER_CREATED", user.getId(), correlationId);
        return new UserAdminDtos.TemporaryPassword(view(user), temporaryPassword);
    }

    @Transactional
    public UserAdminDtos.User update(UUID id, UserAdminDtos.Update body, UUID actorId, String correlationId) {
        UserAccount user = findLocked(id);
        checkVersion(user, body.version());
        String email = email(body.email());
        Set<Role> roles = Set.copyOf(body.roles());
        if (id.equals(actorId) && (!body.enabled() || !roles.contains(Role.ADMIN))) {
            throw new ConflictFailure("Нельзя отключить себя или снять у себя роль ADMIN");
        }
        if (!user.getEmail().equals(email) && users.existsByEmailIgnoreCase(email)) {
            throw new ConflictFailure("Логин уже занят");
        }
        boolean credentialsChanged = !user.getEmail().equals(email)
            || user.isEnabled() != body.enabled() || !user.getRoles().equals(roles);
        user.setEmail(email);
        user.setDisplayName(body.displayName().trim());
        user.setEnabled(body.enabled());
        user.setRoles(roles);
        if (credentialsChanged) invalidateSessions(user);
        users.flush();
        record(actorId, "USER_UPDATED", id, correlationId);
        return view(user);
    }

    @Transactional
    public UserAdminDtos.TemporaryPassword resetPassword(UUID id, UserAdminDtos.ResetPassword body,
                                                         UUID actorId, String correlationId) {
        UserAccount user = findLocked(id);
        checkVersion(user, body.version());
        String temporaryPassword = temporaryPassword();
        user.setPasswordHash(encoder.encode(temporaryPassword));
        user.setPasswordChangeRequired(true);
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        invalidateSessions(user);
        users.flush();
        record(actorId, "USER_PASSWORD_RESET", id, correlationId);
        return new UserAdminDtos.TemporaryPassword(view(user), temporaryPassword);
    }

    private UserAccount findLocked(UUID id) {
        return users.findLocked(id).orElseThrow(() -> new NotFoundFailure("Пользователь не найден"));
    }

    private static void checkVersion(UserAccount user, long version) {
        if (user.getVersion() != version) {
            throw new ConflictFailure("Версия пользователя устарела; обновите данные");
        }
    }

    private void invalidateSessions(UserAccount user) {
        user.incrementAuthVersion();
        sessions.revokeActiveByUser(user.getId(), Instant.now());
    }

    private String temporaryPassword() {
        byte[] bytes = new byte[24];
        random.nextBytes(bytes);
        String password = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        passwordPolicy.validate(password);
        return password;
    }

    private void record(UUID actorId, String event, UUID userId, String correlationId) {
        audit.save(new AuditEvent(UUID.randomUUID(), users.getReferenceById(actorId), event,
            "USER_ACCOUNT", userId, Instant.now(), "Учётная запись " + userId, correlationId));
    }

    private static String email(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private static UserAdminDtos.User view(UserAccount user) {
        return new UserAdminDtos.User(user.getId(), user.getEmail(), user.getDisplayName(),
            user.isEnabled(), user.isPasswordChangeRequired(), Set.copyOf(user.getRoles()),
            user.getLockedUntil(), user.getVersion());
    }
}
