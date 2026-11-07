package dev.noren.staybooking.booking.internal.domain.entity;

import dev.noren.staybooking.booking.internal.domain.DateRange;
import dev.noren.staybooking.bootstrap.domain.Money;
import dev.noren.staybooking.bootstrap.entity.BaseEntity;
import dev.noren.staybooking.bootstrap.exception.InvalidDomainArgumentException;
import dev.noren.staybooking.bootstrap.exception.InvalidDomainTransitionException;
import dev.noren.staybooking.bootstrap.persistence.converter.MoneyConverter;
import dev.noren.staybooking.property.api.view.PropertyBookingView;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "bookings")
public class Booking extends BaseEntity {
    private static final Money MAX_AMOUNT = Money.of(new BigDecimal("9999999999.99"));

    @NotNull
    @Column(nullable = false, name = "property_id", updatable = false)
    private UUID propertyId;

    @NotNull
    @Column(nullable = false, name = "guest_id", updatable = false)
    private UUID guestId;

    @NotNull
    @Column(nullable = false, name = "check_in", updatable = false)
    private LocalDate checkIn;

    @NotNull
    @Column(nullable = false, name = "check_out", updatable = false)
    private LocalDate checkOut;

    @NotNull
    @Positive
    @Column(nullable = false, name = "number_of_guests", updatable = false)
    private Integer numberOfGuests;

    @NotNull
    @Convert(converter = MoneyConverter.class)
    @Column(nullable = false, name = "price_per_night_snapshot", precision = 12, scale = 2, updatable = false)
    private Money pricePerNightSnapshot;

    @NotNull
    @Convert(converter = MoneyConverter.class)
    @Column(nullable = false, name = "total_price", precision = 12, scale = 2, updatable = false)
    private Money totalPrice;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BookingStatus status;

    public static Booking create(
            DateRange dates,
            LocalDate today,
            UUID guestId,
            PropertyBookingView property,
            int numberOfGuests
    ) {
        validateDates(dates, today);
        validateGuestId(guestId);
        validatePropertyFacts(property);
        validateNumberOfGuests(numberOfGuests, property.maxGuests());
        validateNotOwner(guestId, property.hostId());
        validateBookable(property);
        Money price = validatePrice(property.pricePerNight());
        Money total = calculateTotal(price, dates.nights());

        Booking booking = new Booking();
        booking.propertyId = property.id();
        booking.guestId = guestId;
        booking.checkIn = dates.checkIn();
        booking.checkOut = dates.checkOut();
        booking.numberOfGuests = numberOfGuests;
        booking.pricePerNightSnapshot = price;
        booking.totalPrice = total;
        booking.status = BookingStatus.PENDING_PAYMENT;
        return booking;
    }

    private static void validateDates(DateRange dates, LocalDate today) {
        if (dates == null || today == null) {
            throw new InvalidDomainArgumentException("dates and today must not be null");
        }
        if (dates.checkIn().isBefore(today)) {
            throw new InvalidDomainArgumentException("checkIn must be today or later");
        }
    }

    private static void validateGuestId(UUID guestId) {
        if (guestId == null) {
            throw new InvalidDomainArgumentException("guestId must not be null");
        }
    }

    private static void validatePropertyFacts(PropertyBookingView property) {
        if (property == null || property.id() == null || property.hostId() == null) {
            throw new InvalidDomainArgumentException("property booking facts must be complete");
        }
    }

    private static void validateNumberOfGuests(int numberOfGuests, int maxGuests) {
        if (numberOfGuests <= 0 || numberOfGuests > maxGuests) {
            throw new InvalidDomainArgumentException("numberOfGuests must be positive and within property capacity");
        }
    }

    private static void validateNotOwner(UUID guestId, UUID hostId) {
        if (guestId.equals(hostId)) {
            throw new InvalidDomainArgumentException("guest cannot book own property");
        }
    }

    private static void validateBookable(PropertyBookingView property) {
        if (!property.bookable()) {
            throw new InvalidDomainTransitionException("property is not bookable");
        }
    }

    private static Money validatePrice(Money price) {
        if (price == null || price.amount().signum() <= 0 || price.compareTo(MAX_AMOUNT) > 0) {
            throw new InvalidDomainArgumentException("pricePerNight must be positive and fit NUMERIC(12,2)");
        }
        return price;
    }

    private static Money calculateTotal(Money price, long nights) {
        Money total = price.multiply(nights);
        if (total.compareTo(MAX_AMOUNT) > 0) {
            throw new InvalidDomainArgumentException("totalPrice must fit NUMERIC(12,2)");
        }
        return total;
    }
}
