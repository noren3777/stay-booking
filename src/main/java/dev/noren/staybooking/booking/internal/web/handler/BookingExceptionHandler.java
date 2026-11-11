package dev.noren.staybooking.booking.internal.web.handler;

import dev.noren.staybooking.booking.internal.application.exception.BookingConflictException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class BookingExceptionHandler {

    @ExceptionHandler(BookingConflictException.class)
    ProblemDetail handleBookingConflict(BookingConflictException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }
}
