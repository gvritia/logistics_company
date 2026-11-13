package com.lcorp.backend.security;

import java.util.UUID;

public record AuthPrincipal(UUID id, boolean passwordChangeRequired) {
}
