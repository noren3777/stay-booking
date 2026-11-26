package dev.noren.staybooking.booking.internal.web.response;

import dev.noren.staybooking.booking.internal.domain.entity.BookingStatus;
import io.swagger.v3.oas.annotations.media.Schema;

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
        @Schema(description = "Creation currently returns PENDING_PAYMENT; other enum values are reserved for later flows.", allowableValues = "PENDING_PAYMENT") BookingStatus status
) {
}
