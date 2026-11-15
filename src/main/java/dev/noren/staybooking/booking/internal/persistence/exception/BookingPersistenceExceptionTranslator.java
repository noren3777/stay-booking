package dev.noren.staybooking.booking.internal.persistence.exception;

import dev.noren.staybooking.booking.internal.application.exception.BookingConflictException;
import org.postgresql.util.PSQLException;
import org.postgresql.util.ServerErrorMessage;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

@Component
public class BookingPersistenceExceptionTranslator {
    private static final String CONSTRAINT = "ex_bookings_property_occupied_dates";

    public RuntimeException translate(DataIntegrityViolationException ex) {
        for (Throwable cause = ex; cause != null; cause = cause.getCause()) {
            if (cause instanceof PSQLException postgres) {
                ServerErrorMessage error = postgres.getServerErrorMessage();
                if (error != null && CONSTRAINT.equals(error.getConstraint())) {
                    return new BookingConflictException();
                }
            }
        }
        return ex;
    }
}
