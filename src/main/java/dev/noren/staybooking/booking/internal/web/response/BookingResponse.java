package dev.noren.staybooking.booking.internal.web.response;

import dev.noren.staybooking.booking.internal.domain.entity.BookingStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record BookingResponse(
        UUID id,
        UUID propertyId,
        UUID guestId,
        LocalDate checkIn,
        LocalDate checkOut,
        int numberOfGuests,
        BigDecimal pricePerNightSnapshot,
        BigDecimal totalPrice,
        BookingStatus status
) {
}
