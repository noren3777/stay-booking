package dev.noren.staybooking.property.internal.web.request;

import dev.noren.staybooking.property.internal.domain.enums.PropertyStatus;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record PropertyFilterRequest(
        String city,
        @Positive @DecimalMax("9999999999.99") @Digits(integer = 10, fraction = 2) BigDecimal minPrice,
        @Positive @DecimalMax("9999999999.99") @Digits(integer = 10, fraction = 2) BigDecimal maxPrice,
        @Positive Integer minGuests,
        PropertyStatus status
) {
}
