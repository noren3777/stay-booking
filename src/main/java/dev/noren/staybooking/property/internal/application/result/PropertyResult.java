package dev.noren.staybooking.property.internal.application.result;

import dev.noren.staybooking.property.internal.domain.enums.PropertyStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PropertyResult(
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
        List<AmenityResult> amenities,
        List<PropertyImageResult> images
) {
}
