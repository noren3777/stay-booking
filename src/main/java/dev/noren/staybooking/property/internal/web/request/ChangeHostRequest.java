package dev.noren.staybooking.property.internal.web.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ChangeHostRequest(
        @NotNull UUID hostId
) {
}
