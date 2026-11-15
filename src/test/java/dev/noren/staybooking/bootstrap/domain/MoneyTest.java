package dev.noren.staybooking.bootstrap.domain;

import dev.noren.staybooking.bootstrap.exception.InvalidDomainArgumentException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MoneyTest {
    @Test
    void normalizesScaleAndCalculatesWithoutRounding() {
        Money price = Money.of(new BigDecimal("125.5"));

        assertThat(price.amount()).isEqualTo(new BigDecimal("125.50"));
        assertThat(price.multiply(3)).isEqualTo(Money.of(new BigDecimal("376.50")));
        assertThat(Money.of(new BigDecimal("10000000000.00")))
                .isGreaterThan(price);
    }

    @Test
    void rejectsMissingAmountAndExcessFractionalDigits() {
        assertThatThrownBy(() -> Money.of(null))
                .isInstanceOf(InvalidDomainArgumentException.class);
        assertThatThrownBy(() -> Money.of(new BigDecimal("1.001")))
                .isInstanceOf(InvalidDomainArgumentException.class);
    }
}
