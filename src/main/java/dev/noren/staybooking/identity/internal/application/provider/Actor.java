package dev.noren.staybooking.identity.internal.application.provider;

import dev.noren.staybooking.identity.internal.domain.enums.UserRole;

import java.util.UUID;

public record Actor(
        UUID id,
        UserRole role
) {
}
