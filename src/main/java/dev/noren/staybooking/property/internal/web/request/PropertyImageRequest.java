package dev.noren.staybooking.property.internal.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PropertyImageRequest(
        @NotBlank @Size(max = 2048) String url,
        @NotNull Boolean primary
) {
}
