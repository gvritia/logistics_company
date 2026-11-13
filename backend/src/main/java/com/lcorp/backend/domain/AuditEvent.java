package com.lcorp.backend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

// У события входа user может быть null: неуспешный логин не определяет пользователя.
@Entity
@Table(name = "audit_events", schema = "public")
public class AuditEvent {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private UserAccount user;

    @Column(name = "event_type", nullable = false, length = 80)
    private String eventType;

    @Column(name = "entity_type", length = 80)
    private String entityType;

    @Column(name = "entity_id")
    private UUID entityId;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(nullable = false, length = 500)
    private String description;

    @Column(name = "correlation_id", nullable = false, length = 100)
    private String correlationId;

    protected AuditEvent() {
    }

    public AuditEvent(UUID id, UserAccount user, String eventType, String entityType,
                      UUID entityId, Instant occurredAt, String description, String correlationId) {
        this.id = id;
        this.user = user;
        this.eventType = eventType;
        this.entityType = entityType;
        this.entityId = entityId;
        this.occurredAt = occurredAt;
        this.description = description;
        this.correlationId = correlationId;
    }

    public UUID getId() { return id; }
    public UserAccount getUser() { return user; }
    public String getEventType() { return eventType; }
    public String getEntityType() { return entityType; }
    public UUID getEntityId() { return entityId; }
    public Instant getOccurredAt() { return occurredAt; }
    public String getDescription() { return description; }
    public String getCorrelationId() { return correlationId; }
}
