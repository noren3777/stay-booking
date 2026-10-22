package dev.noren.staybooking.property.internal.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateAmenityRequest(
        @NotBlank @Size(max = 100) String name
) {
}
