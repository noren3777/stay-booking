package dev.noren.staybooking.booking.internal.web.controller;

import dev.noren.staybooking.booking.internal.application.result.BookingResult;
import dev.noren.staybooking.booking.internal.application.service.BookingService;
import dev.noren.staybooking.booking.internal.web.mapper.BookingWebMapper;
import dev.noren.staybooking.booking.internal.web.request.CreateBookingRequest;
import dev.noren.staybooking.booking.internal.web.response.BookingResponse;
import dev.noren.staybooking.bootstrap.web.ApiProblemDocument;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
@Tag(name = "Bookings")
public class BookingController {

    private final BookingService service;
    private final BookingWebMapper mapper;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('GUEST')")
    @Operation(summary = "Create a pending booking", description = "GUEST only. Dates use a half-open [checkIn, checkOut) range. Payment and confirmation are separate future use cases. No Location header is returned because there is no booking GET endpoint.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Pending booking created"),
            @ApiResponse(responseCode = "400", description = "Invalid request or dates", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiProblemDocument.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiProblemDocument.class))),
            @ApiResponse(responseCode = "403", description = "GUEST role required", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiProblemDocument.class))),
            @ApiResponse(responseCode = "404", description = "Property not found", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiProblemDocument.class))),
            @ApiResponse(responseCode = "409", description = "Property unavailable or dates occupied", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiProblemDocument.class)))
    })
    public BookingResponse create(@Valid @RequestBody CreateBookingRequest request) {
        BookingResult result = service.create(
                request.propertyId(),
                mapper.toDateRange(request),
                request.numberOfGuests()
        );
        return mapper.toResponse(result);
    }
}
