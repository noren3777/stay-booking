package dev.noren.staybooking.booking.internal.web.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.UUID;

public record CreateBookingRequest(
        @NotNull @Schema(example = "5d876d36-95d7-4d10-8181-ef5b699d625b") UUID propertyId,
        @NotNull @Schema(example = "2030-06-10") LocalDate checkIn,
        @NotNull @Schema(example = "2030-06-12") LocalDate checkOut,
        @NotNull @Positive @Schema(example = "2") Integer numberOfGuests
) {
}
