package dev.noren.staybooking.property.api.view;

import dev.noren.staybooking.bootstrap.domain.Money;

import java.util.UUID;

public record PropertyBookingView(
        UUID id,
        UUID hostId,
        Money pricePerNight,
        int maxGuests,
        boolean bookable
) {
}
