package dev.noren.staybooking.booking.internal.application.exception;

public class BookingConflictException extends RuntimeException {
    public BookingConflictException() {
        super("Dates are unavailable for this property");
    }
}
