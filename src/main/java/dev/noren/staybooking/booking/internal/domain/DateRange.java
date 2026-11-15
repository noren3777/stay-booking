package dev.noren.staybooking.booking.internal.domain;

import dev.noren.staybooking.bootstrap.exception.InvalidDomainArgumentException;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public record DateRange(LocalDate checkIn, LocalDate checkOut) {
    public DateRange {
        if (checkIn == null || checkOut == null) {
            throw new InvalidDomainArgumentException("checkIn and checkOut must not be null");
        }
        if (!checkOut.isAfter(checkIn)) {
            throw new InvalidDomainArgumentException("checkOut must be after checkIn");
        }
    }

    public long nights() {
        return ChronoUnit.DAYS.between(checkIn, checkOut);
    }

    public boolean overlaps(DateRange other) {
        return checkIn.isBefore(other.checkOut) && other.checkIn.isBefore(checkOut);
    }
}
