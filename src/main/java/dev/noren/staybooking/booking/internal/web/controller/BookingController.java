package dev.noren.staybooking.booking.internal.web.controller;

import dev.noren.staybooking.booking.internal.application.result.BookingResult;
import dev.noren.staybooking.booking.internal.application.service.BookingService;
import dev.noren.staybooking.booking.internal.web.mapper.BookingWebMapper;
import dev.noren.staybooking.booking.internal.web.request.CreateBookingRequest;
import dev.noren.staybooking.booking.internal.web.response.BookingResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService service;
    private final BookingWebMapper mapper;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('GUEST')")
    public BookingResponse create(@Valid @RequestBody CreateBookingRequest request) {
        BookingResult result = service.create(
                request.propertyId(),
                mapper.toDateRange(request),
                request.numberOfGuests()
        );
        return mapper.toResponse(result);
    }
}
