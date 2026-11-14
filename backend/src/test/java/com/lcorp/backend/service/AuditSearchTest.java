package com.lcorp.backend.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuditSearchTest {
    @Test
    void acceptsOnlyBoundedPagesAndWhitelistedSort() {
        assertEquals("eventType: ASC,id: ASC",
            new AuditSearch("login", 0, 50, "eventType,asc").safeSort().toString());
        assertThrows(IllegalArgumentException.class,
            () -> new AuditSearch(null, -1, 20, "occurredAt,desc"));
        assertThrows(IllegalArgumentException.class,
            () -> new AuditSearch(null, 0, 51, "occurredAt,desc"));
        assertThrows(IllegalArgumentException.class,
            () -> new AuditSearch("x".repeat(101), 0, 20, "occurredAt,desc"));
        assertThrows(IllegalArgumentException.class,
            () -> new AuditSearch(null, 0, 20, "user.passwordHash,asc"));
    }
}
