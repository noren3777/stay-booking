package dev.noren.staybooking.booking.internal.web.mapper;

import dev.noren.staybooking.booking.internal.application.result.BookingResult;
import dev.noren.staybooking.booking.internal.domain.DateRange;
import dev.noren.staybooking.booking.internal.web.request.CreateBookingRequest;
import dev.noren.staybooking.booking.internal.web.response.BookingResponse;
import dev.noren.staybooking.bootstrap.web.mapper.MoneyWebMapper;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = "spring",
        injectionStrategy = InjectionStrategy.CONSTRUCTOR,
        uses = MoneyWebMapper.class,
        unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface BookingWebMapper {

    DateRange toDateRange(CreateBookingRequest request);

    BookingResponse toResponse(BookingResult result);
}
