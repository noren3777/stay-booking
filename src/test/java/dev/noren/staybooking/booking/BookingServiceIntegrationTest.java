package dev.noren.staybooking.booking;

import dev.noren.staybooking.booking.internal.application.result.BookingResult;
import dev.noren.staybooking.booking.internal.application.service.BookingService;
import dev.noren.staybooking.booking.internal.domain.DateRange;
import dev.noren.staybooking.booking.internal.domain.entity.Booking;
import dev.noren.staybooking.booking.internal.domain.entity.BookingStatus;
import dev.noren.staybooking.booking.internal.persistence.BookingRepository;
import dev.noren.staybooking.bootstrap.domain.FieldChange;
import dev.noren.staybooking.bootstrap.domain.Money;
import dev.noren.staybooking.bootstrap.exception.EntityNotFoundException;
import dev.noren.staybooking.bootstrap.exception.InvalidDomainArgumentException;
import dev.noren.staybooking.bootstrap.exception.InvalidDomainTransitionException;
import dev.noren.staybooking.identity.internal.domain.entity.User;
import dev.noren.staybooking.identity.internal.domain.enums.UserRole;
import dev.noren.staybooking.identity.internal.persistence.UserRepository;
import dev.noren.staybooking.property.api.PropertyApi;
import dev.noren.staybooking.property.internal.domain.entity.Property;
import dev.noren.staybooking.property.internal.domain.input.CreatePropertyDetails;
import dev.noren.staybooking.property.internal.domain.input.PropertyDetailsPatch;
import dev.noren.staybooking.property.internal.persistence.repository.PropertyRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.TestConstructor;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
@RequiredArgsConstructor
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@SpringBootTest(properties = {
        "app.jwt.secret-base64=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="
})
class BookingServiceIntegrationTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 1, 10);

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer("postgres:18.6-bookworm")
                    .withDatabaseName("booking_application_test")
                    .withUsername("test")
                    .withPassword("test");

    private final BookingService service;
    private final BookingRepository bookings;
    private final PropertyRepository properties;
    private final PropertyApi propertyApi;
    private final UserRepository users;
    private final TransactionTemplate transactions;
    private final EntityManager entityManager;

    @TestConfiguration(proxyBeanMethods = false)
    static class FixedClockConfiguration {
        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(Instant.parse("2026-01-10T12:00:00Z"), ZoneOffset.UTC);
        }
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void savesPendingBookingThroughProxyAndKeepsPriceSnapshotAfterPropertyChange() {
        UUID guestId = saveUser(UserRole.GUEST);
        UUID propertyId = saveProperty(true);
        authenticate(guestId, UserRole.GUEST);

        BookingResult result = service.create(propertyId, dates(TODAY, 2), 2);
        assertThat(result.id()).isNotNull();
        assertThat(result.propertyId()).isEqualTo(propertyId);
        assertThat(result.guestId()).isEqualTo(guestId);
        assertThat(result.checkIn()).isEqualTo(TODAY);
        assertThat(result.checkOut()).isEqualTo(TODAY.plusDays(2));
        assertThat(result.numberOfGuests()).isEqualTo(2);
        assertThat(result.pricePerNightSnapshot()).isEqualTo(money("125.50"));
        assertThat(result.totalPrice()).isEqualTo(money("251.00"));
        assertThat(result.status()).isEqualTo(BookingStatus.PENDING_PAYMENT);

        transactions.executeWithoutResult(status -> {
            Property property = properties.findById(propertyId).orElseThrow();
            property.updateDetails(new PropertyDetailsPatch(
                    null, null, null, null, null, new FieldChange<>(true, money("150.00")), null
            ));
        });
        assertThat(propertyApi.findForBooking(propertyId).orElseThrow().pricePerNight())
                .isEqualTo(money("150.00"));

        entityManager.clear();
        Booking saved = bookings.findById(result.id()).orElseThrow();
        assertThat(saved.getPricePerNightSnapshot()).isEqualTo(money("125.50"));
        assertThat(saved.getTotalPrice()).isEqualTo(money("251.00"));
    }

    @Test
    void rejectsMissingAndSoftDeletedProperty() {
        UUID guestId = saveUser(UserRole.GUEST);
        authenticate(guestId, UserRole.GUEST);
        assertThatThrownBy(() -> service.create(UUID.randomUUID(), dates(TODAY, 1), 1))
                .isInstanceOf(EntityNotFoundException.class);

        UUID propertyId = saveProperty(true);
        transactions.executeWithoutResult(status -> properties.findById(propertyId).orElseThrow().delete());
        assertThatThrownBy(() -> service.create(propertyId, dates(TODAY, 1), 1))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void rejectsInactiveAndBlockedProperty() {
        UUID guestId = saveUser(UserRole.GUEST);
        authenticate(guestId, UserRole.GUEST);
        UUID inactiveId = saveProperty(false);
        UUID blockedId = saveProperty(true);
        transactions.executeWithoutResult(status -> properties.findById(blockedId).orElseThrow().block());

        assertThatThrownBy(() -> service.create(inactiveId, dates(TODAY, 1), 1))
                .isInstanceOf(InvalidDomainTransitionException.class);
        assertThatThrownBy(() -> service.create(blockedId, dates(TODAY, 1), 1))
                .isInstanceOf(InvalidDomainTransitionException.class);
    }

    @Test
    void rejectsWrongRoleAndInvalidInputsOutsideHttp() {
        UUID propertyId = saveProperty(true);
        UUID hostId = saveUser(UserRole.HOST);
        authenticate(hostId, UserRole.HOST);
        assertThatThrownBy(() -> service.create(propertyId, dates(TODAY, 1), 1))
                .isInstanceOf(AccessDeniedException.class);

        authenticate(saveUser(UserRole.GUEST), UserRole.GUEST);
        assertThatThrownBy(() -> service.create(null, dates(TODAY, 1), 1))
                .isInstanceOf(InvalidDomainArgumentException.class);
        assertThatThrownBy(() -> service.create(propertyId, dates(TODAY.minusDays(1), 1), 1))
                .isInstanceOf(InvalidDomainArgumentException.class);
        assertThatThrownBy(() -> service.create(propertyId, dates(TODAY, 1), 4))
                .isInstanceOf(InvalidDomainArgumentException.class);
        assertThat(bookings.count()).isZero();
    }

    @Test
    void flushFailureRollsBackBooking() {
        UUID propertyId = saveProperty(true);
        UUID missingGuestId = UUID.randomUUID();
        authenticate(missingGuestId, UserRole.GUEST);

        assertThatThrownBy(() -> service.create(propertyId, dates(TODAY, 1), 1))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(bookings.count()).isZero();
    }

    private UUID saveUser(UserRole role) {
        return users.saveAndFlush(User.create(
                role.name().toLowerCase() + "-" + UUID.randomUUID() + "@example.com", "encoded", role
        )).getId();
    }

    private UUID saveProperty(boolean active) {
        UUID hostId = saveUser(UserRole.HOST);
        Property property = Property.create(hostId, new CreatePropertyDetails(
                "Flat near the park", null, "Ukraine", "Kyiv", "14 Khreshchatyk Street",
                money("125.50"), 3
        ));
        if (active) {
            property.activate();
        }
        return properties.saveAndFlush(property).getId();
    }

    private static void authenticate(UUID id, UserRole role) {
        Jwt jwt = Jwt.withTokenValue("booking-application-test")
                .header("alg", "none")
                .subject(id.toString())
                .claim("role", role.name())
                .build();
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new JwtAuthenticationToken(jwt));
        SecurityContextHolder.setContext(context);
    }

    private static DateRange dates(LocalDate checkIn, int nights) {
        return new DateRange(checkIn, checkIn.plusDays(nights));
    }

    private static Money money(String amount) {
        return Money.of(new BigDecimal(amount));
    }
}
