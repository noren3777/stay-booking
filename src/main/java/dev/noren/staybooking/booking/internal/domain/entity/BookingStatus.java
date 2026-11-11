package dev.noren.staybooking.booking.internal.domain.entity;

import java.util.Set;

public enum BookingStatus {
    PENDING_PAYMENT,
    CONFIRMED,
    PAYMENT_FAILED,
    CANCELLED,
    COMPLETED;

    public static final Set<BookingStatus> OCCUPYING_DATES = Set.of(PENDING_PAYMENT, CONFIRMED);
}
