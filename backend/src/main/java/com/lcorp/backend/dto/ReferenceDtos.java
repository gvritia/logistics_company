package com.lcorp.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class ReferenceDtos {
    private ReferenceDtos() {
    }

    public record CreatePerson(@NotBlank @Size(max = 150) String fullName,
                               @NotBlank @Size(max = 32) String phone) {
    }

    public record Person(long id, String fullName) {
    }

    public record Driver(long id, String fullName, boolean active) {
    }
}
