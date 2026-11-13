package com.lcorp.backend.dto;

import com.lcorp.backend.domain.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public final class AuthDtos {
    private AuthDtos() {
    }

    public record LoginRequest(@NotBlank @Email @Size(max = 254) String email,
                               @NotBlank @Size(max = 128) String password) {
    }

    public record RefreshRequest(@NotBlank @Size(max = 256) String refreshToken) {
    }

    public record ChangePasswordRequest(@NotBlank @Size(max = 128) String currentPassword,
                                        @NotBlank @Size(max = 128) String newPassword) {
    }

    public record CurrentUser(UUID id, String email, String displayName,
                              Set<Role> roles, boolean passwordChangeRequired) {
    }

    public record AuthResponse(String accessToken, String refreshToken,
                               Instant accessExpiresAt, CurrentUser user) {
    }
}
