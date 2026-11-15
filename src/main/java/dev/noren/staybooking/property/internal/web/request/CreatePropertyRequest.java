package dev.noren.staybooking.property.internal.web.request;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record CreatePropertyRequest(
        @NotBlank @Size(max = 255) String title,
        @Size(max = 5000) String description,
        @NotBlank @Size(max = 100) String country,
        @NotBlank @Size(max = 100) String city,
        @NotBlank @Size(max = 255) String address,
        @NotNull @Positive @Digits(integer = 10, fraction = 2) BigDecimal pricePerNight,
        @NotNull @Positive Integer maxGuests
) {
}
