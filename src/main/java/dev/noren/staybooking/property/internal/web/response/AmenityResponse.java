package dev.noren.staybooking.property.internal.web.response;

import java.util.UUID;

public record AmenityResponse(
        UUID id,
        String name
) {
}
