package dev.noren.staybooking.property.internal.web.request;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.openapitools.jackson.nullable.JsonNullable;

import java.math.BigDecimal;

public record UpdatePropertyRequest(
        JsonNullable<@NotBlank @Size(max = 255) String> title,
        JsonNullable<@Size(max = 5000) String> description,
        JsonNullable<@NotBlank @Size(max = 100) String> country,
        JsonNullable<@NotBlank @Size(max = 100) String> city,
        JsonNullable<@NotBlank @Size(max = 255) String> address,
        JsonNullable<@Positive @Digits(integer = 10, fraction = 2) BigDecimal> pricePerNight,
        JsonNullable<@Positive Integer> maxGuests
) {
}
