package com.lcorp.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public final class ClientDtos {
    private ClientDtos() {
    }

    public record Create(@NotBlank @Size(max = 150) String fullName,
                         @Size(max = 32) String phone,
                         @Email @Size(max = 254) String email) {
    }

    public record Update(@NotNull @PositiveOrZero Long version,
                         @NotBlank @Size(max = 150) String fullName,
                         @Size(max = 32) String phone,
                         @Email @Size(max = 254) String email) {
    }

    public record View(long id, String fullName, String phone, String email,
                       boolean active, long version) {
    }
}
