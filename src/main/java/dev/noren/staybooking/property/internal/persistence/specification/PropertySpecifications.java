package dev.noren.staybooking.property.internal.persistence.specification;

import dev.noren.staybooking.property.internal.application.input.PropertyFilterCriteria;
import dev.noren.staybooking.property.internal.domain.entity.Property;
import dev.noren.staybooking.property.internal.domain.enums.PropertyStatus;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.Specification;

import dev.noren.staybooking.bootstrap.domain.Money;
import java.util.Locale;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PropertySpecifications {

    public static Specification<Property> forCatalog(PropertyFilterCriteria filters, PropertyStatus status) {
        return statusIs(status)
                .and(cityIs(filters.city()))
                .and(priceAtLeast(filters.minPrice()))
                .and(priceAtMost(filters.maxPrice()))
                .and(guestsAtLeast(filters.minGuests()));
    }

    private static Specification<Property> statusIs(PropertyStatus status) {
        return (root, query, builder) -> builder.equal(root.get("status"), status);
    }

    private static Specification<Property> cityIs(String city) {
        if (city == null) {
            return Specification.unrestricted();
        }
        String normalizedCity = city.toLowerCase(Locale.ROOT);
        return (root, query, builder) ->
                builder.equal(builder.lower(root.get("city")), normalizedCity);
    }

    private static Specification<Property> priceAtLeast(Money price) {
        if (price == null) {
            return Specification.unrestricted();
        }
        return (root, query, builder) ->
                builder.greaterThanOrEqualTo(root.get("pricePerNight"), price);
    }

    private static Specification<Property> priceAtMost(Money price) {
        if (price == null) {
            return Specification.unrestricted();
        }
        return (root, query, builder) ->
                builder.lessThanOrEqualTo(root.get("pricePerNight"), price);
    }

    private static Specification<Property> guestsAtLeast(Integer guests) {
        if (guests == null) {
            return Specification.unrestricted();
        }
        return (root, query, builder) ->
                builder.greaterThanOrEqualTo(root.get("maxGuests"), guests);
    }
}
