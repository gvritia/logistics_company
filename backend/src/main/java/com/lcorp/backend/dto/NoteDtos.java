package com.lcorp.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public final class NoteDtos {
    private NoteDtos() {
    }

    public record Create(@NotBlank @Size(max = 4000) String body, boolean pinned) {
    }

    public record Update(@NotNull @PositiveOrZero Long version,
                         @NotBlank @Size(max = 4000) String body, boolean pinned) {
    }

    public record View(UUID id, UUID requestId, UUID authorId, String authorName,
                       String body, boolean pinned, Instant createdAt, Instant updatedAt,
                       long version) {
    }
}
