package dev.noren.staybooking.property.internal.domain.input;

import dev.noren.staybooking.bootstrap.domain.Money;

public record CreatePropertyDetails(
        String title,
        String description,
        String country,
        String city,
        String address,
        Money pricePerNight,
        Integer maxGuests
) {
}
