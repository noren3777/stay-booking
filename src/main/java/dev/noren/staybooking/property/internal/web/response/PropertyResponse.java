package dev.noren.staybooking.property.internal.web.response;

import dev.noren.staybooking.property.internal.domain.enums.PropertyStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PropertyResponse(
        UUID id,
        Instant createdAt,
        Instant updatedAt,
        UUID hostId,
        String title,
        String description,
        String country,
        String city,
        String address,
        BigDecimal pricePerNight,
        Integer maxGuests,
        PropertyStatus status,
        Long version,
        List<AmenityResponse> amenities
) {
}
