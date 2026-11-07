package dev.noren.staybooking.property.internal.application.result;

import dev.noren.staybooking.bootstrap.domain.Money;
import dev.noren.staybooking.property.internal.domain.enums.PropertyStatus;

import java.time.Instant;
import java.util.UUID;

public record PropertySummaryResult(
        UUID id,
        Instant createdAt,
        String title,
        String country,
        String city,
        Money pricePerNight,
        Integer maxGuests,
        PropertyStatus status
) {
}
