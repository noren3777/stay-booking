package dev.noren.staybooking.property.internal.domain.input;

import dev.noren.staybooking.bootstrap.domain.FieldChange;
import dev.noren.staybooking.bootstrap.domain.Money;

public record PropertyDetailsPatch(
        FieldChange<String> title,
        FieldChange<String> description,
        FieldChange<String> country,
        FieldChange<String> city,
        FieldChange<String> address,
        FieldChange<Money> pricePerNight,
        FieldChange<Integer> maxGuests
) {
}
