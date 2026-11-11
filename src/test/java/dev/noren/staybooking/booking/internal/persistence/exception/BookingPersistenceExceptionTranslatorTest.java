package dev.noren.staybooking.booking.internal.persistence.exception;

import dev.noren.staybooking.booking.internal.application.exception.BookingConflictException;
import org.junit.jupiter.api.Test;
import org.postgresql.util.PSQLException;
import org.postgresql.util.ServerErrorMessage;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BookingPersistenceExceptionTranslatorTest {
    private final BookingPersistenceExceptionTranslator translator = new BookingPersistenceExceptionTranslator();

    @Test
    void translatesOnlyNamedExclusionViolation() {
        assertThat(translator.translate(wrapped("ex_bookings_property_occupied_dates")))
                .isInstanceOf(BookingConflictException.class)
                .hasMessage("Dates are unavailable for this property");

        for (String other : new String[]{
                "other_exclusion", "fk_bookings_guest", "ck_bookings_dates", "pk_bookings"
        }) {
            DataIntegrityViolationException original = wrapped(other);
            assertThat(translator.translate(original)).isSameAs(original);
        }
        DataIntegrityViolationException unknown = new DataIntegrityViolationException("unknown");
        assertThat(translator.translate(unknown)).isSameAs(unknown);
    }

    private static DataIntegrityViolationException wrapped(String name) {
        PSQLException postgres = mock(PSQLException.class);
        ServerErrorMessage details = mock(ServerErrorMessage.class);
        when(postgres.getServerErrorMessage()).thenReturn(details);
        when(details.getConstraint()).thenReturn(name);
        return new DataIntegrityViolationException("database error", postgres);
    }
}
