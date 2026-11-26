package dev.noren.staybooking.identity.internal.web.response;

import dev.noren.staybooking.identity.internal.domain.enums.UserRole;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        UserRole role,
        Instant createdAt,
        Instant updatedAt
) {
}
