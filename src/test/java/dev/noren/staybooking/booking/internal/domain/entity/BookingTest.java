package dev.noren.staybooking.booking.internal.domain.entity;

import dev.noren.staybooking.booking.internal.domain.DateRange;
import dev.noren.staybooking.bootstrap.domain.Money;
import dev.noren.staybooking.bootstrap.exception.InvalidDomainArgumentException;
import dev.noren.staybooking.bootstrap.exception.InvalidDomainTransitionException;
import dev.noren.staybooking.property.api.view.PropertyBookingView;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BookingTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 1, 10);
    private static final UUID PROPERTY_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID HOST_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID GUEST_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");

    @Test
    void createsPendingBookingTodayWithExactPriceSnapshot() {
        Booking booking = Booking.create(dates(TODAY, 3), TODAY, GUEST_ID,
                property("125.25", true), 2);

        assertThat(booking.getPropertyId()).isEqualTo(PROPERTY_ID);
        assertThat(booking.getGuestId()).isEqualTo(GUEST_ID);
        assertThat(booking.getCheckIn()).isEqualTo(TODAY);
        assertThat(booking.getCheckOut()).isEqualTo(TODAY.plusDays(3));
        assertThat(booking.getNumberOfGuests()).isEqualTo(2);
        assertThat(booking.getPricePerNightSnapshot()).isEqualTo(money("125.25"));
        assertThat(booking.getTotalPrice()).isEqualTo(money("375.75"));
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.PENDING_PAYMENT);

        Booking futureBooking = Booking.create(dates(TODAY.plusDays(1), 1), TODAY, GUEST_ID,
                property("10.00", true), 1);
        assertThat(futureBooking.getCheckIn()).isEqualTo(TODAY.plusDays(1));
    }

    @Test
    void rejectsPastCheckInAndMissingDateInputs() {
        assertInvalid(() -> Booking.create(dates(TODAY.minusDays(1), 1), TODAY,
                GUEST_ID, property("10.00", true), 1));
        assertInvalid(() -> Booking.create(null, TODAY, GUEST_ID,
                property("10.00", true), 1));
        assertInvalid(() -> Booking.create(dates(TODAY, 1), null, GUEST_ID,
                property("10.00", true), 1));
    }

    @Test
    void rejectsInvalidGuestsAndIncompletePropertyFacts() {
        assertInvalid(() -> Booking.create(dates(TODAY, 1), TODAY, GUEST_ID,
                property("10.00", true), 0));
        assertInvalid(() -> Booking.create(dates(TODAY, 1), TODAY, GUEST_ID,
                property("10.00", true), 3));
        assertInvalid(() -> Booking.create(dates(TODAY, 1), TODAY, null,
                property("10.00", true), 1));
        assertInvalid(() -> Booking.create(dates(TODAY, 1), TODAY, GUEST_ID,
                null, 1));
        assertInvalid(() -> Booking.create(dates(TODAY, 1), TODAY, GUEST_ID,
                new PropertyBookingView(null, HOST_ID, money("10.00"), 2, true), 1));
        assertInvalid(() -> Booking.create(dates(TODAY, 1), TODAY, GUEST_ID,
                new PropertyBookingView(PROPERTY_ID, null, money("10.00"), 2, true), 1));
    }

    @Test
    void rejectsOwnAndNonBookableProperty() {
        assertInvalid(() -> Booking.create(dates(TODAY, 1), TODAY, HOST_ID,
                property("10.00", true), 1));
        assertThatThrownBy(() -> Booking.create(dates(TODAY, 1), TODAY, GUEST_ID,
                property("10.00", false), 1))
                .isInstanceOf(InvalidDomainTransitionException.class);
    }

    @Test
    void rejectsMissingZeroAndOutOfRangeNightlyPrice() {
        for (Money price : new Money[]{null, money("0.00"), money("-1.00"),
                money("10000000000.00")}) {
            assertInvalid(() -> Booking.create(dates(TODAY, 1), TODAY, GUEST_ID,
                    new PropertyBookingView(PROPERTY_ID, HOST_ID, price, 2, true), 1));
        }
    }

    @Test
    void acceptsMaximumTotalAndRejectsOverflow() {
        Booking maximumNightlyPrice = Booking.create(dates(TODAY, 1), TODAY, GUEST_ID,
                property("9999999999.99", true), 1);
        Booking atLimit = Booking.create(dates(TODAY, 3), TODAY, GUEST_ID,
                property("3333333333.33", true), 1);

        assertThat(maximumNightlyPrice.getTotalPrice()).isEqualTo(money("9999999999.99"));
        assertThat(atLimit.getTotalPrice()).isEqualTo(money("9999999999.99"));
        assertInvalid(() -> Booking.create(dates(TODAY, 3), TODAY, GUEST_ID,
                property("3333333333.34", true), 1));
    }

    @Test
    void keepsPriceSnapshotWhenLaterPropertyViewHasDifferentPrice() {
        PropertyBookingView original = property("100.00", true);
        Booking booking = Booking.create(dates(TODAY, 2), TODAY, GUEST_ID, original, 1);
        PropertyBookingView changed = property("150.00", true);

        assertThat(changed.pricePerNight()).isNotEqualTo(original.pricePerNight());
        assertThat(booking.getPricePerNightSnapshot()).isEqualTo(money("100.00"));
        assertThat(booking.getTotalPrice()).isEqualTo(money("200.00"));
    }

    private static DateRange dates(LocalDate checkIn, int nights) {
        return new DateRange(checkIn, checkIn.plusDays(nights));
    }

    private static PropertyBookingView property(String price, boolean bookable) {
        return new PropertyBookingView(PROPERTY_ID, HOST_ID, money(price), 2, bookable);
    }

    private static Money money(String amount) {
        return Money.of(new BigDecimal(amount));
    }

    private static void assertInvalid(Runnable action) {
        assertThatThrownBy(action::run).isInstanceOf(InvalidDomainArgumentException.class);
    }
}
