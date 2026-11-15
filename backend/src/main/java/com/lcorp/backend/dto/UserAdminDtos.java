package com.lcorp.backend.dto;

import com.lcorp.backend.domain.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class UserAdminDtos {
    private UserAdminDtos() {
    }

    public record Create(@NotBlank @Email @Size(max = 254) String email,
                         @NotBlank @Size(max = 160) String displayName,
                         @NotEmpty Set<@NotNull Role> roles) {
    }

    public record Update(@NotBlank @Email @Size(max = 254) String email,
                         @NotBlank @Size(max = 160) String displayName,
                         @NotNull Boolean enabled,
                         @NotEmpty Set<@NotNull Role> roles,
                         @NotNull @PositiveOrZero Long version) {
    }

    public record ResetPassword(@NotNull @PositiveOrZero Long version) {
    }

    public record User(UUID id, String email, String displayName, boolean enabled,
                       boolean passwordChangeRequired, Set<Role> roles,
                       Instant lockedUntil, long version) {
    }

    public record Page(List<User> items, int page, int size, long totalElements, int totalPages) {
    }

    public record TemporaryPassword(User user, String temporaryPassword) {
    }
}
