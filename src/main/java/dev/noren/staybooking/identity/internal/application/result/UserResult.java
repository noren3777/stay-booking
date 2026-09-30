package dev.noren.staybooking.identity.internal.application.result;

import dev.noren.staybooking.identity.internal.domain.enums.UserRole;

import java.time.Instant;
import java.util.UUID;

public record UserResult(
        UUID id,
        String email,
        UserRole role,
        Instant createdAt,
        Instant updatedAt
) {
}
