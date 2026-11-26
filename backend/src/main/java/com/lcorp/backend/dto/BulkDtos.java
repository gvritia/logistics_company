package com.lcorp.backend.dto;

import com.lcorp.backend.domain.TransportationRequestStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public final class BulkDtos {
    private BulkDtos() {
    }

    public enum Action {ARCHIVE, RESTORE, CHANGE_STATUS, ADD_TAG, REMOVE_TAG}

    public record Target(@NotNull UUID id, @NotNull @PositiveOrZero Long version) {
    }

    public record Request(@NotEmpty @Size(max = 100) List<@NotNull @Valid Target> items,
                          @NotNull Action action,
                          TransportationRequestStatus status,
                          @Size(max = 50) String tag) {
    }

    public record Success(UUID id, long version, TransportationRequestStatus status,
                          boolean archived) {
    }

    public record Failure(UUID id, String code, String message) {
    }

    public record Result(int succeeded, int failed, List<Success> successes,
                         List<Failure> failures) {
    }
}
