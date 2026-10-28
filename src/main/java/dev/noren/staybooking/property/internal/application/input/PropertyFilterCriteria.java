package dev.noren.staybooking.property.internal.application.input;

import dev.noren.staybooking.bootstrap.exception.InvalidDomainArgumentException;
import dev.noren.staybooking.property.internal.domain.enums.PropertyStatus;

import java.math.BigDecimal;

public record PropertyFilterCriteria(
        String city,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        Integer minGuests,
        PropertyStatus status
) {
    private static final BigDecimal MAX_PRICE = new BigDecimal("9999999999.99");

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

    private static void validatePrice(BigDecimal price, String field) {
        if (price == null) {
            return;
        }
        if (price.signum() <= 0) {
            throw new InvalidDomainArgumentException(field + " must be positive");
        }
        if (price.scale() > 2) {
            throw new InvalidDomainArgumentException(field + " must have at most 2 decimal places");
        }
        if (price.compareTo(MAX_PRICE) > 0) {
            throw new InvalidDomainArgumentException(field + " must not exceed " + MAX_PRICE);
        }
    }
}
