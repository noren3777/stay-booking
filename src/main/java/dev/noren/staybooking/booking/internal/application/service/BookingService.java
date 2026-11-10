package dev.noren.staybooking.booking.internal.application.service;

import dev.noren.staybooking.booking.internal.application.authorization.BookingAuthorization;
import dev.noren.staybooking.booking.internal.application.exception.BookingConflictException;
import dev.noren.staybooking.booking.internal.application.mapper.BookingApplicationMapper;
import dev.noren.staybooking.booking.internal.application.result.BookingResult;
import dev.noren.staybooking.booking.internal.domain.DateRange;
import dev.noren.staybooking.booking.internal.domain.entity.Booking;
import dev.noren.staybooking.booking.internal.domain.entity.BookingStatus;
import dev.noren.staybooking.booking.internal.domain.input.CreateBookingDetails;
import dev.noren.staybooking.booking.internal.persistence.BookingRepository;
import dev.noren.staybooking.bootstrap.exception.EntityNotFoundException;
import dev.noren.staybooking.bootstrap.exception.InvalidDomainArgumentException;
import dev.noren.staybooking.identity.api.view.ActorView;
import dev.noren.staybooking.property.api.PropertyApi;
import dev.noren.staybooking.property.api.view.PropertyBookingView;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingAuthorization authorization;
    private final PropertyApi propertyApi;
    private final BookingRepository bookings;
    private final BookingApplicationMapper mapper;
    private final Clock clock;

    @Transactional
    public BookingResult create(UUID propertyId, DateRange dates, int numberOfGuests) {
        ActorView actor = authorization.requireCurrentGuest();
        if (propertyId == null) {
            throw new InvalidDomainArgumentException("propertyId must not be null");
        }

        PropertyBookingView property = propertyApi.findForBooking(propertyId)
                .orElseThrow(() -> new EntityNotFoundException("Property not found"));

        CreateBookingDetails details = mapper.toCreateDetails(
                dates, LocalDate.now(clock), actor.id(), property, numberOfGuests
        );
        Booking booking = Booking.create(details);

        boolean overlapping = bookings.existsOverlapping(
                propertyId, booking.getCheckIn(), booking.getCheckOut(), BookingStatus.OCCUPYING_DATES
        );
        if (overlapping) {
            throw new BookingConflictException();
        }

        Booking saved = bookings.saveAndFlush(booking);

        return mapper.toResult(saved);
    }
}
