package dev.noren.staybooking.booking.internal.web.handler;

import dev.noren.staybooking.booking.internal.application.exception.BookingConflictException;
import dev.noren.staybooking.bootstrap.web.ApiProblem;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class BookingExceptionHandler {

    @ExceptionHandler(BookingConflictException.class)
    ProblemDetail handleBookingConflict(BookingConflictException ex, HttpServletRequest request) {
        return ApiProblem.of(HttpStatus.CONFLICT, HttpStatus.CONFLICT.name(), ex.getMessage(), request);
    }
}
