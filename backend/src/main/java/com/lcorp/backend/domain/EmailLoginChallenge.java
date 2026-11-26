package com.lcorp.backend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "email_login_challenges", schema = "public")
public class EmailLoginChallenge {
    @Id
    @Column(length = 254)
    private String email;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "auth_version", nullable = false)
    private long authVersion;

    @Column(nullable = false)
    private boolean enrollment;

    @Column(name = "code_hash", nullable = false, length = 64)
    private String codeHash;

    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt;

    @Column(name = "attempts_left", nullable = false)
    private int attemptsLeft;

    protected EmailLoginChallenge() {
    }

    public void issue(UUID userId, long authVersion, boolean enrollment, String codeHash, Instant now) {
        this.userId = userId;
        this.authVersion = authVersion;
        this.enrollment = enrollment;
        this.codeHash = codeHash;
        this.issuedAt = now;
        this.attemptsLeft = 5;
    }

    public boolean matches(UUID userId, long authVersion, boolean enrollment) {
        return Objects.equals(this.userId, userId) && this.authVersion == authVersion && this.enrollment == enrollment;
    }

    public Instant getIssuedAt() {
        return issuedAt;
    }

    public String getCodeHash() {
        return codeHash;
    }

    public boolean hasAttempts() {
        return attemptsLeft > 0;
    }

    public void failAttempt() {
        attemptsLeft--;
    }

    public void consume() {
        attemptsLeft = 0;
    }
}
