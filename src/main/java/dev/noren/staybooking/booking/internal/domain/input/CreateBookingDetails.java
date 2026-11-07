package dev.noren.staybooking.booking.internal.domain.input;

import dev.noren.staybooking.booking.internal.domain.DateRange;
import dev.noren.staybooking.bootstrap.domain.Money;

import java.time.LocalDate;
import java.util.UUID;

public record CreateBookingDetails(
        DateRange dates,
        LocalDate today,
        UUID guestId,
        UUID propertyId,
        UUID hostId,
        Money pricePerNight,
        int maxGuests,
        boolean bookable,
        int numberOfGuests
) {
}
