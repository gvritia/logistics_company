package com.lcorp.backend.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class AuditDtos {
    private AuditDtos() {
    }

    public record Entry(UUID id, UUID userId, String userEmail, String eventType,
                        String entityType, UUID entityId, Instant occurredAt,
                        String description, String correlationId) {
    }

    public record Page(List<Entry> items, int page, int size, long totalElements, int totalPages) {
    }
}
