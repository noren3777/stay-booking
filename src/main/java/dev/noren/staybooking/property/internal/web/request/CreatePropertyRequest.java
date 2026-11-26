package dev.noren.staybooking.property.internal.web.request;

import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

public record CreatePropertyRequest(
        @NotBlank @Size(max = 255) @Schema(example = "Quiet apartment") String title,
        @Size(max = 5000) String description,
        @NotBlank @Size(max = 100) @Schema(example = "Ukraine") String country,
        @NotBlank @Size(max = 100) @Schema(example = "Kyiv") String city,
        @NotBlank @Size(max = 255) @Schema(example = "Khreshchatyk 1") String address,
        @NotNull @Positive @Digits(integer = 10, fraction = 2) @Schema(example = "120.00") BigDecimal pricePerNight,
        @NotNull @Positive @Schema(example = "4") Integer maxGuests
) {
}
