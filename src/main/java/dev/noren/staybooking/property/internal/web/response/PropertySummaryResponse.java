package dev.noren.staybooking.property.internal.web.response;

import dev.noren.staybooking.property.internal.domain.enums.PropertyStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PropertySummaryResponse(
        UUID id,
        Instant createdAt,
        String title,
        String country,
        String city,
        BigDecimal pricePerNight,
        Integer maxGuests,
        PropertyStatus status
) {
}
