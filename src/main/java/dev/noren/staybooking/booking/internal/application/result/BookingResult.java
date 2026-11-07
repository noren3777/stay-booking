package dev.noren.staybooking.booking.internal.application.result;

import dev.noren.staybooking.booking.internal.domain.entity.BookingStatus;
import dev.noren.staybooking.bootstrap.domain.Money;

import java.time.LocalDate;
import java.util.UUID;

public record BookingResult(
        UUID id,
        UUID propertyId,
        UUID guestId,
        LocalDate checkIn,
        LocalDate checkOut,
        int numberOfGuests,
        Money pricePerNightSnapshot,
        Money totalPrice,
        BookingStatus status
) {
}
