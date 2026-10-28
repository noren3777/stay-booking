package dev.noren.staybooking.property.internal.application.input;

import dev.noren.staybooking.bootstrap.exception.InvalidDomainArgumentException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PropertyFilterCriteriaTest {

    @Test
    void rejectsInvalidPricesWithoutWebValidation() {
        for (BigDecimal price : List.of(
                BigDecimal.ZERO,
                new BigDecimal("-1"),
                new BigDecimal("1.001"),
                new BigDecimal("10000000000.00")
        )) {
            assertThatThrownBy(() -> new PropertyFilterCriteria(null, price, null, null, null))
                    .isInstanceOf(InvalidDomainArgumentException.class);
            assertThatThrownBy(() -> new PropertyFilterCriteria(null, null, price, null, null))
                    .isInstanceOf(InvalidDomainArgumentException.class);
        }
    }

    @Test
    void rejectsInvalidCapacityAndCityWithoutWebValidation() {
        assertThatThrownBy(() -> new PropertyFilterCriteria(null, null, null, 0, null))
                .isInstanceOf(InvalidDomainArgumentException.class);
        assertThatThrownBy(() -> new PropertyFilterCriteria("x".repeat(101), null, null, null, null))
                .isInstanceOf(InvalidDomainArgumentException.class);
    }

    @Test
    void acceptsBoundaryValuesAndNormalizesCity() {
        var criteria = new PropertyFilterCriteria(
                "  Kyiv  ", new BigDecimal("0.01"), new BigDecimal("9999999999.99"), 1, null
        );

        assertThat(criteria.city()).isEqualTo("Kyiv");
        assertThat(criteria.minPrice()).isEqualByComparingTo("0.01");
        assertThat(criteria.maxPrice()).isEqualByComparingTo("9999999999.99");
        assertThat(criteria.minGuests()).isEqualTo(1);
    }
}
