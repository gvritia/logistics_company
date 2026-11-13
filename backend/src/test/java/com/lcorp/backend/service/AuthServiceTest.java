package com.lcorp.backend.service;

import com.lcorp.backend.domain.Role;
import com.lcorp.backend.domain.UserAccount;
import com.lcorp.backend.error.AuthFailure;
import com.lcorp.backend.repository.AuditEventRepository;
import com.lcorp.backend.repository.RefreshSessionRepository;
import com.lcorp.backend.repository.UserAccountRepository;
import com.lcorp.backend.security.JwtService;
import com.lcorp.backend.security.PasswordPolicy;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthServiceTest {
    @Test
    void locksAccountAfterFiveFailuresAndResetsAfterLockExpires() {
        var encoder = new BCryptPasswordEncoder(4);
        UserAccount user = new UserAccount(UUID.randomUUID(), "admin@example.org", "Admin",
            encoder.encode("correct-password"), Set.of(Role.ADMIN));
        UserAccountRepository users = mock(UserAccountRepository.class);
        when(users.findForLogin("admin@example.org")).thenReturn(Optional.of(user));
        String secret = Base64.getEncoder().encodeToString(new byte[32]);
        AuthService auth = new AuthService(users, mock(RefreshSessionRepository.class),
            mock(AuditEventRepository.class), encoder, new PasswordPolicy(),
            new JwtService(secret, "test", 15, 14), 5, 15);

        for (int i = 0; i < 5; i++) {
            assertThrows(AuthFailure.class, () -> auth.login("admin@example.org", "wrong", "test"));
        }
        assertEquals(5, user.getFailedLoginAttempts());
        assertNotNull(user.getLockedUntil());
        assertThrows(AuthFailure.class, () -> auth.login("admin@example.org", "correct-password", "test"));

        user.setLockedUntil(Instant.now().minusSeconds(1));
        auth.login("admin@example.org", "correct-password", "test");
        assertEquals(0, user.getFailedLoginAttempts());
        assertNull(user.getLockedUntil());
    }
}
