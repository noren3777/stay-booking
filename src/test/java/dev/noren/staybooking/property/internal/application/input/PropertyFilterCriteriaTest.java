package dev.noren.staybooking.property.internal.application.input;

import dev.noren.staybooking.bootstrap.domain.Money;
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
                new BigDecimal("-1")
        )) {
            assertThatThrownBy(() -> new PropertyFilterCriteria(null, Money.of(price), null, null, null))
                    .isInstanceOf(InvalidDomainArgumentException.class);
            assertThatThrownBy(() -> new PropertyFilterCriteria(null, null, Money.of(price), null, null))
                    .isInstanceOf(InvalidDomainArgumentException.class);
        }
        assertThatThrownBy(() -> Money.of(new BigDecimal("1.001")))
                .isInstanceOf(InvalidDomainArgumentException.class);
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
                "  Kyiv  ", Money.of(new BigDecimal("0.01")), Money.of(new BigDecimal("10000000000.00")), 1, null
        );

        assertThat(criteria.city()).isEqualTo("Kyiv");
        assertThat(criteria.minPrice().amount()).isEqualByComparingTo("0.01");
        assertThat(criteria.maxPrice().amount()).isEqualByComparingTo("10000000000.00");
        assertThat(criteria.minGuests()).isEqualTo(1);
    }
}
