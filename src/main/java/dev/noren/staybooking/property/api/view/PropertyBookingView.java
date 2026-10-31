package dev.noren.staybooking.property.api.view;

import java.math.BigDecimal;
import java.util.UUID;

public record PropertyBookingView(
        UUID id,
        UUID hostId,
        BigDecimal pricePerNight,
        int maxGuests,
        boolean bookable
) {
}
