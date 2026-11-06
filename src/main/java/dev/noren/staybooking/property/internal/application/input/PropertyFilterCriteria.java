package dev.noren.staybooking.property.internal.application.input;

import dev.noren.staybooking.bootstrap.exception.InvalidDomainArgumentException;
import dev.noren.staybooking.bootstrap.domain.Money;
import dev.noren.staybooking.property.internal.domain.enums.PropertyStatus;

public record PropertyFilterCriteria(
        String city,
        Money minPrice,
        Money maxPrice,
        Integer minGuests,
        PropertyStatus status
) {
    public PropertyFilterCriteria {
        if (city != null) {
            city = city.trim();
            if (city.isBlank()) {
                throw new InvalidDomainArgumentException("city must not be blank");
            }
            if (city.length() > 100) {
                throw new InvalidDomainArgumentException("city must not exceed 100 characters");
            }
        }
        validatePrice(minPrice, "minPrice");
        validatePrice(maxPrice, "maxPrice");
        if (minGuests != null && minGuests <= 0) {
            throw new InvalidDomainArgumentException("minGuests must be positive");
        }
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw new InvalidDomainArgumentException("minPrice must not exceed maxPrice");
        }
    }

    private static void validatePrice(Money price, String field) {
        if (price == null) {
            return;
        }
        if (price.amount().signum() <= 0) {
            throw new InvalidDomainArgumentException(field + " must be positive");
        }
    }
}
