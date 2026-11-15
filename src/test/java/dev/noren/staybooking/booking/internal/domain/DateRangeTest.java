package dev.noren.staybooking.booking.internal.domain;

import dev.noren.staybooking.bootstrap.exception.InvalidDomainArgumentException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DateRangeTest {
    private static final LocalDate JANUARY_1 = LocalDate.of(2026, 1, 1);

    @Test
    void countsNightsAcrossMonthBoundary() {
        LocalDate checkIn = LocalDate.of(2026, 1, 31);

        assertThat(new DateRange(checkIn, LocalDate.of(2026, 2, 1)).nights()).isEqualTo(1);
        assertThat(new DateRange(checkIn, LocalDate.of(2026, 2, 3)).nights()).isEqualTo(3);
    }

    @Test
    void acceptsHistoricalDatesBecauseTodayIsNotPartOfTheRangeInvariant() {
        assertThat(new DateRange(LocalDate.of(2000, 1, 1), LocalDate.of(2000, 1, 2)).nights())
                .isEqualTo(1);
    }

    @Test
    void rejectsMissingAndUnorderedDates() {
        assertThatThrownBy(() -> new DateRange(null, JANUARY_1))
                .isInstanceOf(InvalidDomainArgumentException.class);
        assertThatThrownBy(() -> new DateRange(JANUARY_1, null))
                .isInstanceOf(InvalidDomainArgumentException.class);
        assertThatThrownBy(() -> new DateRange(JANUARY_1, JANUARY_1))
                .isInstanceOf(InvalidDomainArgumentException.class);
        assertThatThrownBy(() -> new DateRange(JANUARY_1.plusDays(1), JANUARY_1))
                .isInstanceOf(InvalidDomainArgumentException.class);
    }

    @Test
    void equalDatesProduceEqualValuesAndHashCodes() {
        DateRange first = januaryRange(1, 3);
        DateRange same = januaryRange(1, 3);

        assertThat(first).isEqualTo(same).hasSameHashCodeAs(same);
    }

    @Test
    void adjacentRangesDoNotOverlapInEitherDirection() {
        DateRange first = januaryRange(1, 3);
        DateRange next = januaryRange(3, 5);

        assertThat(first.overlaps(next)).isFalse();
        assertThat(next.overlaps(first)).isFalse();
    }

    @Test
    void equalNestedAndPartialRangesOverlapInBothDirections() {
        DateRange base = januaryRange(2, 6);
        DateRange equal = januaryRange(2, 6);
        DateRange nested = januaryRange(3, 5);
        DateRange leftPartial = januaryRange(1, 3);
        DateRange rightPartial = januaryRange(5, 7);

        for (DateRange other : new DateRange[]{equal, nested, leftPartial, rightPartial}) {
            assertThat(base.overlaps(other)).isTrue();
            assertThat(other.overlaps(base)).isTrue();
        }
    }

    @Test
    void separatedRangesDoNotOverlap() {
        DateRange first = januaryRange(1, 2);
        DateRange later = januaryRange(4, 5);

        assertThat(first.overlaps(later)).isFalse();
        assertThat(later.overlaps(first)).isFalse();
    }

    private static DateRange januaryRange(int checkInDay, int checkOutDay) {
        return new DateRange(LocalDate.of(2026, 1, checkInDay),
                LocalDate.of(2026, 1, checkOutDay));
    }
}
