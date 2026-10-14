package dev.noren.staybooking.property.internal.domain.input;

import dev.noren.staybooking.bootstrap.domain.FieldChange;

import java.math.BigDecimal;

public record PropertyDetailsPatch(
        FieldChange<String> title,
        FieldChange<String> description,
        FieldChange<String> country,
        FieldChange<String> city,
        FieldChange<String> address,
        FieldChange<BigDecimal> pricePerNight,
        FieldChange<Integer> maxGuests
) {
}
