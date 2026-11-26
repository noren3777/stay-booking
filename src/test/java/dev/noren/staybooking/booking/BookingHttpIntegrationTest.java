package dev.noren.staybooking.booking;

import dev.noren.staybooking.booking.internal.domain.entity.Booking;
import dev.noren.staybooking.booking.internal.domain.entity.BookingStatus;
import dev.noren.staybooking.booking.internal.persistence.BookingRepository;
import dev.noren.staybooking.booking.internal.web.response.BookingResponse;
import dev.noren.staybooking.bootstrap.domain.Money;
import dev.noren.staybooking.identity.IdentityBookingTestFixtures;
import dev.noren.staybooking.identity.api.view.ActorRole;
import dev.noren.staybooking.property.PropertyBookingTestFixtures;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

@Testcontainers
@AutoConfigureRestTestClient
@RequiredArgsConstructor
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "app.jwt.secret-base64=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
                "app.jwt.issuer=booking-http-test"
        }
)
class BookingHttpIntegrationTest {

    private static final String PASSWORD = "Integration-password-31";
    private static final LocalDate TODAY = LocalDate.of(2026, 1, 10);

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer("postgres:18.6-bookworm")
                    .withDatabaseName("booking_http_test")
                    .withUsername("test")
                    .withPassword("test");

    private final RestTestClient http;
    @MockitoSpyBean
    private BookingRepository bookings;
    @LocalServerPort
    private int port;
    private final PasswordEncoder encoder;
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

    @Test
    void guestCreatesPendingBookingAndStoredSnapshotMatchesResponse() {
        IdentityBookingTestFixtures.TestUser guest = saveUser(ActorRole.GUEST);
        UUID propertyId = saveProperty(true);

        var exchange = http.post().uri("/api/bookings")
                .headers(headers -> headers.setBearerAuth(login(guest)))
                .contentType(MediaType.APPLICATION_JSON)
                .body(request(propertyId, TODAY, TODAY.plusDays(2), 2))
                .exchange().expectStatus().isCreated()
                .expectBody(BookingResponse.class).returnResult();

        assertThat(exchange.getResponseHeaders().getLocation()).isNull();
        BookingResponse response = exchange.getResponseBody();
        assertThat(response).isNotNull();
        assertThat(response.id()).isNotNull();
        assertThat(response.propertyId()).isEqualTo(propertyId);
        assertThat(response.guestId()).isEqualTo(guest.id());
        assertThat(response.checkIn()).isEqualTo(TODAY);
        assertThat(response.checkOut()).isEqualTo(TODAY.plusDays(2));
        assertThat(response.numberOfGuests()).isEqualTo(2);
        assertThat(response.pricePerNightSnapshot()).isEqualByComparingTo("125.50");
        assertThat(response.totalPrice()).isEqualByComparingTo("251.00");
        assertThat(response.status()).isEqualTo(BookingStatus.PENDING_PAYMENT);

        entityManager.clear();
        Booking stored = bookings.findById(response.id()).orElseThrow();
        assertThat(stored.getGuestId()).isEqualTo(response.guestId());
        assertThat(stored.getPropertyId()).isEqualTo(response.propertyId());
        assertThat(stored.getCheckIn()).isEqualTo(response.checkIn());
        assertThat(stored.getCheckOut()).isEqualTo(response.checkOut());
        assertThat(stored.getNumberOfGuests()).isEqualTo(response.numberOfGuests());
        assertThat(stored.getPricePerNightSnapshot().amount()).isEqualByComparingTo(response.pricePerNightSnapshot());
        assertThat(stored.getTotalPrice().amount()).isEqualByComparingTo(response.totalPrice());
        assertThat(stored.getStatus()).isEqualTo(response.status());
    }

    @Test
    void authenticationAndGuestRoleAreRequired() {
        long bookingsBefore = bookings.count();
        UUID propertyId = saveProperty(true);
        Map<String, Object> body = request(propertyId, TODAY, TODAY.plusDays(1), 1);

        http.post().uri("/api/bookings").contentType(MediaType.APPLICATION_JSON)
                .body(body).exchange().expectStatus().isUnauthorized()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody().jsonPath("$.status").isEqualTo(401)
                .jsonPath("$.code").isEqualTo("UNAUTHORIZED")
                .jsonPath("$.title").isEqualTo("Unauthorized")
                .jsonPath("$.path").isEqualTo("/api/bookings")
                .jsonPath("$.requestId").exists()
                .jsonPath("$.detail").isEqualTo("Authentication required");
        http.post().uri("/api/bookings")
                .headers(headers -> headers.setBearerAuth(login(saveUser(ActorRole.HOST))))
                .contentType(MediaType.APPLICATION_JSON)
                .body(body).exchange().expectStatus().isForbidden()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody().jsonPath("$.status").isEqualTo(403)
                .jsonPath("$.code").isEqualTo("FORBIDDEN")
                .jsonPath("$.title").isEqualTo("Forbidden")
                .jsonPath("$.path").isEqualTo("/api/bookings")
                .jsonPath("$.requestId").exists()
                .jsonPath("$.detail").isEqualTo("Access denied");
        assertThat(bookings.count()).isEqualTo(bookingsBefore);
    }

    @Test
    void requestShapeAndBusinessDatesReturnStableBadRequest() {
        long bookingsBefore = bookings.count();
        IdentityBookingTestFixtures.TestUser guest = saveUser(ActorRole.GUEST);
        String token = login(guest);
        UUID propertyId = saveProperty(true);

        http.post().uri("/api/bookings")
                .headers(headers -> headers.setBearerAuth(token))
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("checkIn", TODAY, "checkOut", TODAY.plusDays(1), "numberOfGuests", 1))
                .exchange().expectStatus().isBadRequest().expectBody()
                .jsonPath("$.status").isEqualTo(400)
                .jsonPath("$.code").isEqualTo("VALIDATION_FAILED")
                .jsonPath("$.title").isEqualTo("Bad Request")
                .jsonPath("$.path").isEqualTo("/api/bookings")
                .jsonPath("$.requestId").exists()
                .jsonPath("$.detail").isEqualTo("Request validation failed")
                .jsonPath("$.errors[0].field").isEqualTo("propertyId")
                .jsonPath("$.errors[0].message").exists();

        http.post().uri("/api/bookings")
                .headers(headers -> headers.setBearerAuth(token))
                .contentType(MediaType.APPLICATION_JSON)
                .body(request(propertyId, TODAY, TODAY.plusDays(1), 0))
                .exchange().expectStatus().isBadRequest().expectBody()
                .jsonPath("$.status").isEqualTo(400)
                .jsonPath("$.code").isEqualTo("VALIDATION_FAILED");

        http.post().uri("/api/bookings")
                .headers(headers -> headers.setBearerAuth(token))
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"propertyId\":")
                .exchange().expectStatus().isBadRequest().expectBody()
                .jsonPath("$.code").isEqualTo("BAD_REQUEST")
                .jsonPath("$.detail").isEqualTo("Request is invalid")
                .jsonPath("$.path").isEqualTo("/api/bookings")
                .jsonPath("$.requestId").exists();

        http.post().uri("/api/bookings")
                .headers(headers -> headers.setBearerAuth(token))
                .contentType(MediaType.APPLICATION_JSON)
                .body(request(propertyId, TODAY, TODAY.plusDays(1), 4))
                .exchange().expectStatus().isBadRequest().expectBody()
                .jsonPath("$.status").isEqualTo(400)
                .jsonPath("$.detail").isEqualTo("numberOfGuests must be positive and within property capacity");

        for (LocalDate checkIn : new LocalDate[]{TODAY.minusDays(1), TODAY.plusDays(1)}) {
            http.post().uri("/api/bookings")
                    .headers(headers -> headers.setBearerAuth(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request(propertyId, checkIn, TODAY.plusDays(1), 1))
                    .exchange().expectStatus().isBadRequest().expectBody()
                    .jsonPath("$.status").isEqualTo(400);
        }
        assertThat(bookings.count()).isEqualTo(bookingsBefore);
    }

    @Test
    void missingAndSoftDeletedPropertyReturnNotFound() {
        long bookingsBefore = bookings.count();
        String token = login(saveUser(ActorRole.GUEST));
        assertNotFound(token, UUID.randomUUID());

        UUID deletedId = saveProperty(true);
        transactions.executeWithoutResult(status -> PropertyBookingTestFixtures.delete(entityManager, deletedId));
        assertNotFound(token, deletedId);
        assertThat(bookings.count()).isEqualTo(bookingsBefore);
    }

    @Test
    void inactiveAndBlockedPropertyReturnConflict() {
        long bookingsBefore = bookings.count();
        String token = login(saveUser(ActorRole.GUEST));
        UUID inactiveId = saveProperty(false);
        UUID blockedId = saveProperty(true);
        transactions.executeWithoutResult(status -> PropertyBookingTestFixtures.block(entityManager, blockedId));

        assertConflict(token, inactiveId);
        assertConflict(token, blockedId);
        assertThat(bookings.count()).isEqualTo(bookingsBefore);
    }

    @Test
    void existingOverlapReturnsStableConflictProblem() {
        String token = login(saveUser(ActorRole.GUEST));
        UUID propertyId = saveProperty(true);
        Map<String, Object> body = request(propertyId, TODAY, TODAY.plusDays(2), 1);

        http.post().uri("/api/bookings")
                .headers(headers -> headers.setBearerAuth(token))
                .contentType(MediaType.APPLICATION_JSON)
                .body(body).exchange().expectStatus().isCreated()
                .expectBody(BookingResponse.class);

        http.post().uri("/api/bookings")
                .headers(headers -> headers.setBearerAuth(token))
                .contentType(MediaType.APPLICATION_JSON)
                .body(body).exchange().expectStatus().isEqualTo(409)
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody().jsonPath("$.status").isEqualTo(409)
                .jsonPath("$.code").isEqualTo("CONFLICT")
                .jsonPath("$.title").isEqualTo("Conflict")
                .jsonPath("$.path").isEqualTo("/api/bookings")
                .jsonPath("$.requestId").exists()
                .jsonPath("$.detail").isEqualTo("Dates are unavailable for this property");
    }

    @Test
    void concurrentHttpCreatesReturnCreatedAndSafeConflict() throws Exception {
        String token = login(saveUser(ActorRole.GUEST));
        UUID propertyId = saveProperty(true);
        long before = bookings.count();
        CountDownLatch bothChecked = new CountDownLatch(2);
        doAnswer(invocation -> {
            bothChecked.countDown();
            assertThat(bothChecked.await(10, TimeUnit.SECONDS)).isTrue();
            return false;
        }).when(bookings).existsOverlapping(any(), any(), any(), any());

        String body = """
                {"propertyId":"%s","checkIn":"2026-02-10","checkOut":"2026-02-12","numberOfGuests":1}
                """.formatted(propertyId);
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/bookings"))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body)).build();
        try (HttpClient client = HttpClient.newHttpClient(); var workers = Executors.newFixedThreadPool(2)) {
            var completions = new ExecutorCompletionService<HttpResponse<String>>(workers);
            completions.submit(() -> client.send(request, HttpResponse.BodyHandlers.ofString()));
            completions.submit(() -> client.send(request, HttpResponse.BodyHandlers.ofString()));
            HttpResponse<String> first = completions.poll(15, TimeUnit.SECONDS).get();
            HttpResponse<String> second = completions.poll(15, TimeUnit.SECONDS).get();
            assertThat(new int[]{first.statusCode(), second.statusCode()}).containsExactlyInAnyOrder(201, 409);
            HttpResponse<String> conflict = first.statusCode() == 409 ? first : second;
            assertThat(conflict.headers().firstValue("Content-Type").orElseThrow())
                    .contains("application/problem+json");
            assertThat(conflict.body()).contains("Dates are unavailable for this property")
                    .doesNotContain("ex_bookings_property_occupied_dates", "23P01");
        }
        assertThat(bookings.count()).isEqualTo(before + 1);
    }

    private void assertNotFound(String token, UUID propertyId) {
        http.post().uri("/api/bookings")
                .headers(headers -> headers.setBearerAuth(token))
                .contentType(MediaType.APPLICATION_JSON)
                .body(request(propertyId, TODAY, TODAY.plusDays(1), 1))
                .exchange().expectStatus().isNotFound().expectBody()
                .jsonPath("$.status").isEqualTo(404)
                .jsonPath("$.code").isEqualTo("NOT_FOUND")
                .jsonPath("$.title").isEqualTo("Not Found")
                .jsonPath("$.path").isEqualTo("/api/bookings")
                .jsonPath("$.requestId").exists()
                .jsonPath("$.detail").isEqualTo("Property not found");
    }

    private void assertConflict(String token, UUID propertyId) {
        http.post().uri("/api/bookings")
                .headers(headers -> headers.setBearerAuth(token))
                .contentType(MediaType.APPLICATION_JSON)
                .body(request(propertyId, TODAY, TODAY.plusDays(1), 1))
                .exchange().expectStatus().isEqualTo(409).expectBody()
                .jsonPath("$.status").isEqualTo(409)
                .jsonPath("$.detail").isEqualTo("property is not bookable");
    }

    private IdentityBookingTestFixtures.TestUser saveUser(ActorRole role) {
        return transactions.execute(status -> IdentityBookingTestFixtures.persistUser(
                entityManager, role, encoder.encode(PASSWORD)
        ));
    }

    private UUID saveProperty(boolean active) {
        UUID hostId = saveUser(ActorRole.HOST).id();
        return transactions.execute(status -> PropertyBookingTestFixtures.persistProperty(
                entityManager, hostId, Money.of(new BigDecimal("125.50")), active
        ));
    }

    private String login(IdentityBookingTestFixtures.TestUser user) {
        Map<?, ?> result = http.post().uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("email", user.email(), "password", PASSWORD))
                .exchange().expectStatus().isOk()
                .expectBody(Map.class).returnResult().getResponseBody();
        assertThat(result).isNotNull();
        return (String) result.get("token");
    }

    private static Map<String, Object> request(UUID propertyId, LocalDate checkIn, LocalDate checkOut, int guests) {
        return Map.of(
                "propertyId", propertyId,
                "checkIn", checkIn,
                "checkOut", checkOut,
                "numberOfGuests", guests
        );
    }
}
