package dev.noren.staybooking.booking.internal.domain.entity;

import dev.noren.staybooking.booking.internal.domain.DateRange;
import dev.noren.staybooking.booking.internal.domain.input.CreateBookingDetails;
import dev.noren.staybooking.booking.internal.persistence.BookingRepository;
import dev.noren.staybooking.bootstrap.domain.Money;
import dev.noren.staybooking.identity.internal.domain.entity.User;
import dev.noren.staybooking.identity.internal.domain.enums.UserRole;
import dev.noren.staybooking.identity.internal.persistence.UserRepository;
import dev.noren.staybooking.property.internal.domain.entity.Property;
import dev.noren.staybooking.property.internal.domain.input.CreatePropertyDetails;
import dev.noren.staybooking.property.internal.persistence.repository.PropertyRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestConstructor;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
@Testcontainers
@RequiredArgsConstructor
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class BookingMappingTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer("postgres:18.6-bookworm")
                    .withDatabaseName("booking_mapping_test")
                    .withUsername("test")
                    .withPassword("test");

    private final UserRepository users;
    private final PropertyRepository properties;
    private final BookingRepository bookings;
    private final EntityManager entityManager;
    private final JdbcTemplate jdbc;

    private enum InvalidBookingValue {
        NUMBER_OF_GUESTS, PRICE_PER_NIGHT, TOTAL_PRICE, STATUS
    }

    @Test
    void reloadsBookingWithInheritedIdTimestampsAndPriceSnapshots() {
        Booking booking = saveBooking();
        UUID bookingId = booking.getId();
        UUID propertyId = booking.getPropertyId();
        UUID guestId = booking.getGuestId();
        entityManager.clear();

        Booking loaded = bookings.findById(bookingId).orElseThrow();

        assertThat(loaded.getPropertyId()).isEqualTo(propertyId);
        assertThat(loaded.getGuestId()).isEqualTo(guestId);
        assertThat(loaded.getCheckIn()).isEqualTo(LocalDate.of(2026, 1, 10));
        assertThat(loaded.getCheckOut()).isEqualTo(LocalDate.of(2026, 1, 12));
        assertThat(loaded.getNumberOfGuests()).isEqualTo(2);
        assertThat(loaded.getPricePerNightSnapshot()).isEqualTo(money("125.50"));
        assertThat(loaded.getTotalPrice()).isEqualTo(money("251.00"));
        assertThat(loaded.getStatus()).isEqualTo(BookingStatus.PENDING_PAYMENT);
        assertThat(loaded.getCreatedAt()).isNotNull();
        assertThat(loaded.getUpdatedAt()).isNotNull();
    }

    @Test
    void databaseRejectsInvalidDateOrder() {
        UUID bookingId = saveBooking().getId();

        assertThatThrownBy(() -> jdbc.update(
                "UPDATE bookings SET check_out = check_in WHERE id = ?", bookingId
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void databaseRejectsMissingGuestReference() {
        UUID bookingId = saveBooking().getId();

        assertThatThrownBy(() -> jdbc.update(
                "UPDATE bookings SET guest_id = ? WHERE id = ?", UUID.randomUUID(), bookingId
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @ParameterizedTest
    @EnumSource(InvalidBookingValue.class)
    void databaseRejectsInvalidBookingValues(InvalidBookingValue invalidValue) {
        UUID bookingId = saveBooking().getId();

        assertThatThrownBy(() -> {
            switch (invalidValue) {
                case NUMBER_OF_GUESTS -> jdbc.update("UPDATE bookings SET number_of_guests = 0 WHERE id = ?", bookingId);
                case PRICE_PER_NIGHT -> jdbc.update("UPDATE bookings SET price_per_night_snapshot = 0 WHERE id = ?", bookingId);
                case TOTAL_PRICE -> jdbc.update("UPDATE bookings SET total_price = -1 WHERE id = ?", bookingId);
                case STATUS -> jdbc.update("UPDATE bookings SET status = 'UNKNOWN' WHERE id = ?", bookingId);
            }
        })
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void databaseRejectsMissingPropertyReference() {
        UUID bookingId = saveBooking().getId();

        assertThatThrownBy(() -> jdbc.update(
                "UPDATE bookings SET property_id = ? WHERE id = ?", UUID.randomUUID(), bookingId
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    private Booking saveBooking() {
        UUID hostId = users.saveAndFlush(User.create(
                "host-" + UUID.randomUUID() + "@example.com", "encoded", UserRole.HOST
        )).getId();
        UUID guestId = users.saveAndFlush(User.create(
                "guest-" + UUID.randomUUID() + "@example.com", "encoded", UserRole.GUEST
        )).getId();
        Property property = Property.create(hostId, new CreatePropertyDetails(
                "Flat near the park", null, "Ukraine", "Kyiv", "14 Khreshchatyk Street",
                money("125.50"), 3
        ));
        property.activate();
        UUID propertyId = properties.saveAndFlush(property).getId();

        Booking booking = Booking.create(new CreateBookingDetails(
                new DateRange(LocalDate.of(2026, 1, 10), LocalDate.of(2026, 1, 12)),
                LocalDate.of(2026, 1, 10), guestId, propertyId, hostId,
                money("125.50"), 3, true, 2
        ));
        return bookings.saveAndFlush(booking);
    }

    private static Money money(String amount) {
        return Money.of(new BigDecimal(amount));
    }
}
