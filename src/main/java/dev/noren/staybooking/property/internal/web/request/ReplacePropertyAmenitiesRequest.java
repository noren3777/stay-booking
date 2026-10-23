package dev.noren.staybooking.property.internal.web.request;

import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record ReplacePropertyAmenitiesRequest(
        @NotNull List<@NotNull UUID> amenityIds
) {
}
