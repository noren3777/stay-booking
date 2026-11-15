package dev.noren.staybooking.property.internal.web.request;

import dev.noren.staybooking.property.internal.domain.enums.PropertyStatus;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record PropertyFilterRequest(
        String city,
        @Positive BigDecimal minPrice,
        @Positive BigDecimal maxPrice,
        @Positive Integer minGuests,
        PropertyStatus status
) {
}
