package com.lcorp.backend.service;

import com.lcorp.backend.domain.Role;
import com.lcorp.backend.domain.UserAccount;
import com.lcorp.backend.dto.UserAdminDtos;
import com.lcorp.backend.error.ConflictFailure;
import com.lcorp.backend.repository.AuditEventRepository;
import com.lcorp.backend.repository.RefreshSessionRepository;
import com.lcorp.backend.repository.UserAccountRepository;
import com.lcorp.backend.security.PasswordPolicy;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserAdminServiceTest {
    private final UserAccountRepository users = mock(UserAccountRepository.class);
    private final RefreshSessionRepository sessions = mock(RefreshSessionRepository.class);
    private final AuditEventRepository audit = mock(AuditEventRepository.class);
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private final UserAdminService service = new UserAdminService(users, sessions, audit, encoder,
        new PasswordPolicy());
    private final UUID actorId = UUID.randomUUID();

    @Test
    void createAndResetGenerateDifferentTemporaryPasswordsAndRevokeSessions() {
        when(users.saveAndFlush(any(UserAccount.class))).thenAnswer(call -> call.getArgument(0));
        UserAdminDtos.TemporaryPassword created = service.create(
            new UserAdminDtos.Create(" USER@EXAMPLE.ORG ", " User ", Set.of(Role.VIEWER)),
            actorId, "test");
        ArgumentCaptor<UserAccount> saved = ArgumentCaptor.forClass(UserAccount.class);
        verify(users).saveAndFlush(saved.capture());
        UserAccount user = saved.getValue();
        assertEquals("user@example.org", user.getEmail());
        assertEquals("User", user.getDisplayName());
        assertTrue(user.isPasswordChangeRequired());
        assertTrue(encoder.matches(created.temporaryPassword(), user.getPasswordHash()));
        assertFalse(created.temporaryPassword().equals(user.getPasswordHash()));

        user.setPasswordChangeRequired(false);
        user.setFailedLoginAttempts(5);
        user.setLockedUntil(Instant.now().plusSeconds(3600));
        when(users.findLocked(user.getId())).thenReturn(Optional.of(user));
        UserAdminDtos.TemporaryPassword reset = service.resetPassword(user.getId(),
            new UserAdminDtos.ResetPassword(0L), actorId, "test");

        assertNotEquals(created.temporaryPassword(), reset.temporaryPassword());
        assertTrue(encoder.matches(reset.temporaryPassword(), user.getPasswordHash()));
        assertFalse(encoder.matches(created.temporaryPassword(), user.getPasswordHash()));
        assertTrue(reset.user().passwordChangeRequired());
        assertEquals(0, user.getFailedLoginAttempts());
        assertEquals(null, user.getLockedUntil());
        assertEquals(1, user.getAuthVersion());
        verify(sessions).revokeActiveByUser(org.mockito.ArgumentMatchers.eq(user.getId()),
            org.mockito.ArgumentMatchers.any(Instant.class));
    }

    @Test
    void protectsCurrentAdminAndRejectsStaleVersion() {
        UserAccount actor = new UserAccount(actorId, "admin@example.org", "Admin", "hash", Set.of(Role.ADMIN));
        when(users.findLocked(actorId)).thenReturn(Optional.of(actor));

        assertThrows(ConflictFailure.class, () -> service.update(actorId,
            new UserAdminDtos.Update("admin@example.org", "Admin", false, Set.of(Role.ADMIN), 0L),
            actorId, "test"));
        assertThrows(ConflictFailure.class, () -> service.update(actorId,
            new UserAdminDtos.Update("admin@example.org", "Admin", true, Set.of(Role.VIEWER), 0L),
            actorId, "test"));
        assertThrows(ConflictFailure.class, () -> service.resetPassword(actorId,
            new UserAdminDtos.ResetPassword(1L), actorId, "test"));
        assertTrue(actor.isEnabled());
        assertEquals(Set.of(Role.ADMIN), actor.getRoles());
        assertEquals(0, actor.getAuthVersion());
    }

    @Test
    void limitsPageAndSortInputs() {
        assertThrows(IllegalArgumentException.class, () -> service.list(-1, 20, "email,asc"));
        assertThrows(IllegalArgumentException.class, () -> service.list(0, 51, "email,asc"));
        assertThrows(IllegalArgumentException.class, () -> service.list(0, 20, "passwordHash,asc"));
    }
}
