package com.lcorp.backend.service;

import com.lcorp.backend.domain.AuditEvent;
import com.lcorp.backend.domain.Role;
import com.lcorp.backend.domain.UserAccount;
import com.lcorp.backend.repository.AuditEventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuditReadServiceTest {
    @Test
    void mapsNullableActorWithoutReturningEntity() {
        var events = mock(AuditEventRepository.class);
        UUID id = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        UserAccount actor = new UserAccount(actorId, "admin@example.org", "Admin", "secret-hash",
            Set.of(Role.ADMIN));
        AuditEvent anonymous = new AuditEvent(id, null, "LOGIN_FAILED", "USER_ACCOUNT", null,
            Instant.parse("2026-11-14T10:00:00Z"), "Неудачный вход", "test-correlation");
        AuditEvent known = new AuditEvent(UUID.randomUUID(), actor, "REQUEST_CREATED",
            "TRANSPORTATION_REQUEST", UUID.randomUUID(), Instant.parse("2026-11-14T10:01:00Z"),
            "Заявка создана", "other-correlation");
        when(events.findAll(any(Specification.class), any(Pageable.class))).thenReturn(
            new PageImpl<>(List.of(anonymous, known), PageRequest.of(0, 20), 2));

        var page = new AuditReadService(events).list(new AuditSearch(null, 0, 20, "occurredAt,desc"));

        assertEquals(2, page.totalElements());
        assertEquals(2, page.items().size());
        assertEquals(id, page.items().getFirst().id());
        assertNull(page.items().getFirst().userId());
        assertNull(page.items().getFirst().userEmail());
        assertEquals(actorId, page.items().get(1).userId());
        assertEquals("admin@example.org", page.items().get(1).userEmail());
        assertEquals("other-correlation", page.items().get(1).correlationId());
    }
}
