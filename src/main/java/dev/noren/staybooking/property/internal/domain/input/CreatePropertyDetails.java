package dev.noren.staybooking.property.internal.domain.input;

import java.math.BigDecimal;

public record CreatePropertyDetails(
        String title,
        String description,
        String country,
        String city,
        String address,
        BigDecimal pricePerNight,
        Integer maxGuests
) {
}
