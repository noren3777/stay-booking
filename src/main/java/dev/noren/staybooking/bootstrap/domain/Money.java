package dev.noren.staybooking.bootstrap.domain;

import dev.noren.staybooking.bootstrap.exception.InvalidDomainArgumentException;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record Money(BigDecimal amount) implements Comparable<Money> {
    public static final int SCALE = 2;

    public Money {
        if (amount == null) {
            throw new InvalidDomainArgumentException("amount must not be null");
        }
        if (amount.scale() > SCALE) {
            throw new InvalidDomainArgumentException("amount must have at most " + SCALE + " decimal places");
        }
        amount = amount.setScale(SCALE, RoundingMode.UNNECESSARY);
    }

    public static Money of(BigDecimal amount) {
        return new Money(amount);
    }

    public Money multiply(long multiplier) {
        return new Money(amount.multiply(BigDecimal.valueOf(multiplier)));
    }

    @Override
    public int compareTo(Money other) {
        return amount.compareTo(other.amount);
    }
}
