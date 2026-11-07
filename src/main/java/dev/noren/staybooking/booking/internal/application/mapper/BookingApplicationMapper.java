package dev.noren.staybooking.booking.internal.application.mapper;

import dev.noren.staybooking.booking.internal.application.result.BookingResult;
import dev.noren.staybooking.booking.internal.domain.DateRange;
import dev.noren.staybooking.booking.internal.domain.entity.Booking;
import dev.noren.staybooking.booking.internal.domain.input.CreateBookingDetails;
import dev.noren.staybooking.property.api.view.PropertyBookingView;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.time.LocalDate;
import java.util.UUID;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface BookingApplicationMapper {

    BookingResult toResult(Booking booking);

    @Mapping(target = "propertyId", source = "property.id")
    CreateBookingDetails toCreateDetails(
            DateRange dates, LocalDate today, UUID guestId, PropertyBookingView property, int numberOfGuests
    );

}
