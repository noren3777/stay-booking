package dev.noren.staybooking.booking.internal.domain.entity;

import dev.noren.staybooking.booking.internal.domain.DateRange;
import dev.noren.staybooking.booking.internal.domain.input.CreateBookingDetails;
import dev.noren.staybooking.bootstrap.domain.Money;
import dev.noren.staybooking.bootstrap.exception.InvalidDomainArgumentException;
import dev.noren.staybooking.bootstrap.exception.InvalidDomainTransitionException;
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
        Booking booking = Booking.create(details(dates(TODAY, 3), TODAY, GUEST_ID,
                money("125.25"), true, 2));

        assertThat(booking.getPropertyId()).isEqualTo(PROPERTY_ID);
        assertThat(booking.getGuestId()).isEqualTo(GUEST_ID);
        assertThat(booking.getCheckIn()).isEqualTo(TODAY);
        assertThat(booking.getCheckOut()).isEqualTo(TODAY.plusDays(3));
        assertThat(booking.getNumberOfGuests()).isEqualTo(2);
        assertThat(booking.getPricePerNightSnapshot()).isEqualTo(money("125.25"));
        assertThat(booking.getTotalPrice()).isEqualTo(money("375.75"));
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.PENDING_PAYMENT);

        Booking futureBooking = Booking.create(details(dates(TODAY.plusDays(1), 1), TODAY,
                GUEST_ID, money("10.00"), true, 1));
        assertThat(futureBooking.getCheckIn()).isEqualTo(TODAY.plusDays(1));
    }

    @Test
    void rejectsPastCheckInAndMissingDateInputs() {
        assertInvalid(() -> Booking.create(details(dates(TODAY.minusDays(1), 1), TODAY,
                GUEST_ID, money("10.00"), true, 1)));
        assertInvalid(() -> Booking.create(details(null, TODAY,
                GUEST_ID, money("10.00"), true, 1)));
        assertInvalid(() -> Booking.create(details(dates(TODAY, 1), null,
                GUEST_ID, money("10.00"), true, 1)));
    }

    @Test
    void rejectsInvalidGuestsAndIncompleteInputs() {
        assertInvalid(() -> Booking.create(details(dates(TODAY, 1), TODAY,
                GUEST_ID, money("10.00"), true, 0)));
        assertInvalid(() -> Booking.create(details(dates(TODAY, 1), TODAY,
                GUEST_ID, money("10.00"), true, 3)));
        assertInvalid(() -> Booking.create(details(dates(TODAY, 1), TODAY,
                null, money("10.00"), true, 1)));
        assertInvalid(() -> Booking.create(new CreateBookingDetails(
                dates(TODAY, 1), TODAY, GUEST_ID, null, HOST_ID, money("10.00"), 2, true, 1
        )));
        assertInvalid(() -> Booking.create(new CreateBookingDetails(
                dates(TODAY, 1), TODAY, GUEST_ID, PROPERTY_ID, null, money("10.00"), 2, true, 1
        )));
    }

    @Test
    void rejectsOwnAndNonBookableProperty() {
        assertInvalid(() -> Booking.create(details(dates(TODAY, 1), TODAY,
                HOST_ID, money("10.00"), true, 1)));
        assertThatThrownBy(() -> Booking.create(details(dates(TODAY, 1), TODAY,
                GUEST_ID, money("10.00"), false, 1)))
                .isInstanceOf(InvalidDomainTransitionException.class);
    }

    @Test
    void rejectsMissingZeroAndOutOfRangeNightlyPrice() {
        for (Money price : new Money[]{null, money("0.00"), money("-1.00"),
                money("10000000000.00")}) {
            assertInvalid(() -> Booking.create(details(dates(TODAY, 1), TODAY,
                    GUEST_ID, price, true, 1)));
        }
    }

    @Test
    void acceptsMaximumTotalAndRejectsOverflow() {
        Booking maximumNightlyPrice = Booking.create(details(dates(TODAY, 1), TODAY,
                GUEST_ID, money("9999999999.99"), true, 1));
        Booking atLimit = Booking.create(details(dates(TODAY, 3), TODAY,
                GUEST_ID, money("3333333333.33"), true, 1));

        assertThat(maximumNightlyPrice.getTotalPrice()).isEqualTo(money("9999999999.99"));
        assertThat(atLimit.getTotalPrice()).isEqualTo(money("9999999999.99"));
        assertInvalid(() -> Booking.create(details(dates(TODAY, 3), TODAY,
                GUEST_ID, money("3333333333.34"), true, 1)));
    }

    @Test
    void keepsPriceSnapshotWhenLaterInputHasDifferentPrice() {
        CreateBookingDetails original = details(dates(TODAY, 2), TODAY,
                GUEST_ID, money("100.00"), true, 1);
        Booking booking = Booking.create(original);
        CreateBookingDetails changed = details(dates(TODAY, 2), TODAY,
                GUEST_ID, money("150.00"), true, 1);

        assertThat(changed.pricePerNight()).isNotEqualTo(original.pricePerNight());
        assertThat(booking.getPricePerNightSnapshot()).isEqualTo(money("100.00"));
        assertThat(booking.getTotalPrice()).isEqualTo(money("200.00"));
    }

    private static DateRange dates(LocalDate checkIn, int nights) {
        return new DateRange(checkIn, checkIn.plusDays(nights));
    }

    private static CreateBookingDetails details(
            DateRange dates, LocalDate today, UUID guestId, Money price,
            boolean bookable, int numberOfGuests
    ) {
        return new CreateBookingDetails(
                dates, today, guestId, PROPERTY_ID, HOST_ID, price, 2, bookable, numberOfGuests
        );
    }

    private static Money money(String amount) {
        return Money.of(new BigDecimal(amount));
    }

    private static void assertInvalid(Runnable action) {
        assertThatThrownBy(action::run).isInstanceOf(InvalidDomainArgumentException.class);
    }
}
