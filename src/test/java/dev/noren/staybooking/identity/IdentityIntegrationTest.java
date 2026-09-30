package dev.noren.staybooking.identity;

import dev.noren.staybooking.identity.internal.application.result.TokenResult;
import dev.noren.staybooking.identity.internal.application.result.UserResult;
import dev.noren.staybooking.identity.internal.domain.entity.User;
import dev.noren.staybooking.identity.internal.domain.enums.UserRole;
import dev.noren.staybooking.identity.internal.persistence.UserRepository;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
@AutoConfigureRestTestClient
@RequiredArgsConstructor
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@ExtendWith(OutputCaptureExtension.class)
@Execution(ExecutionMode.SAME_THREAD)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Import(IdentityIntegrationTest.AdminProbe.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "app.jwt.secret-base64=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
                "app.jwt.issuer=identity-integration-test",
                "app.jwt.ttl=PT1H"
        }
)
class IdentityIntegrationTest {

    private static final String PASSWORD = "Integration-password-31";

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer("postgres:18.6-bookworm")
                    .withDatabaseName("identity_integration_test")
                    .withUsername("test")
                    .withPassword("test");

    private final RestTestClient http;
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtDecoder jwtDecoder;

    @BeforeEach
    void cleanDatabase() {
        users.deleteAllInBatch();
    }

    @Test
    void registrationPersistsNormalizedUserWithPasswordHash() {
        var response = http.post()
                .uri("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "email", "Alice@Email.com",
                        "password", PASSWORD
                ))
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.id").exists()
                .jsonPath("$.email").isEqualTo("alice@email.com")
                .jsonPath("$.role").isEqualTo("GUEST")
                .jsonPath("$.createdAt").exists()
                .jsonPath("$.updatedAt").exists()
                .jsonPath("$.password").doesNotExist()
                .jsonPath("$.passwordHash").doesNotExist()
                .jsonPath("$.password_hash").doesNotExist()
                .returnResult();

        User stored = users.findByEmail("alice@email.com")
                .orElseThrow();

        assertThat(stored.getId()).isNotNull();
        assertThat(stored.getRole()).isEqualTo(UserRole.GUEST);
        assertThat(stored.getPasswordHash()).isNotEqualTo(PASSWORD);
        assertThat(encoder.matches(PASSWORD, stored.getPasswordHash()))
                .isTrue();

        assertThat(body(response.getResponseBody()))
                .doesNotContain(PASSWORD, stored.getPasswordHash());
    }

    @Test
    void duplicateNormalizedEmailReturns409() {
        register("Alice@Email.com");

        var response = http.post()
                .uri("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "email", "alice@email.com",
                        "password", PASSWORD
                ))
                .exchange()
                .expectStatus().isEqualTo(409)
                .expectBody()
                .jsonPath("$.detail").isEqualTo("Email already exists")
                .returnResult();

        assertThat(users.count()).isEqualTo(1);

        assertThat(body(response.getResponseBody()))
                .doesNotContain(
                        PASSWORD,
                        "uc_users_email",
                        "org.hibernate",
                        "insert into users"
                );
    }

    @Test
    void databaseRejectsDuplicateEmailWithoutApplicationService() {
        String hash = encoder.encode(PASSWORD);

        users.saveAndFlush(
                User.create("unique@email.com", hash, UserRole.GUEST)
        );

        assertThatThrownBy(() ->
                users.saveAndFlush(
                        User.create(
                                "unique@email.com",
                                hash,
                                UserRole.HOST
                        )
                ))
                .isInstanceOf(DataIntegrityViolationException.class);

        assertThat(users.count()).isEqualTo(1);
    }

    @Test
    void concurrentRegistrationsCreateExactlyOneUser() throws Exception {
        var barrier = new CyclicBarrier(2);
        var executor = Executors.newFixedThreadPool(2);

        Callable<Integer> request = () -> {
            barrier.await(5, TimeUnit.SECONDS);

            return http.post()
                    .uri("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "email", "race@email.com",
                            "password", PASSWORD
                    ))
                    .exchange()
                    .returnResult(String.class)
                    .getStatus()
                    .value();
        };

        try {
            var first = executor.submit(request);
            var second = executor.submit(request);

            assertThat(List.of(
                    first.get(20, TimeUnit.SECONDS),
                    second.get(20, TimeUnit.SECONDS)
            )).containsExactlyInAnyOrder(201, 409);
        } finally {
            executor.shutdownNow();
        }

        assertThat(users.count()).isEqualTo(1);
        assertThat(users.findByEmail("race@email.com")).isPresent();
    }

    @Test
    void loginReturnsJwtThatWorksForCurrentUser() {
        UserResult created = register("alice@email.com");
        String token = login("ALICE@email.com");

        var jwt = jwtDecoder.decode(token);

        assertThat(jwt.getSubject()).isEqualTo(created.id().toString());
        assertThat(jwt.getClaimAsString("role")).isEqualTo("GUEST");
        assertThat(jwt.getClaimAsString("iss"))
                .isEqualTo("identity-integration-test");
        assertThat(jwt.getExpiresAt()).isAfter(Instant.now());
        assertThat(jwt.getClaims()).doesNotContainKeys(
                "password", "passwordHash", "password_hash", "email"
        );

        http.get()
                .uri("/api/users/me")
                .headers(headers -> headers.setBearerAuth(token))
                .exchange()
                .expectStatus().isOk()
                .expectHeader().doesNotExist("Set-Cookie")
                .expectBody()
                .jsonPath("$.id").isEqualTo(created.id().toString())
                .jsonPath("$.email").isEqualTo("alice@email.com")
                .jsonPath("$.role").isEqualTo("GUEST")
                .jsonPath("$.passwordHash").doesNotExist();

        http.get()
                .uri("/api/users/me")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void missingOrInvalidTokenReturns401() {
        http.get()
                .uri("/api/users/me")
                .exchange()
                .expectStatus().isUnauthorized();

        http.get()
                .uri("/api/users/me")
                .headers(headers -> headers.setBearerAuth("not-a-jwt"))
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void wrongPasswordAndUnknownUserReturn401() {
        register("alice@email.com");

        for (String email : List.of(
                "alice@email.com",
                "unknown@email.com"
        )) {
            http.post()
                    .uri("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "email", email,
                            "password", "Wrong-password-42"
                    ))
                    .exchange()
                    .expectStatus().isUnauthorized();
        }
    }

    @Test
    void adminEndpointReturns403ForGuestAnd200ForAdmin() {
        register("guest@email.com");
        String guestToken = login("guest@email.com");

        http.get()
                .uri("/api/admin/test-probe")
                .headers(headers -> headers.setBearerAuth(guestToken))
                .exchange()
                .expectStatus().isForbidden();

        users.saveAndFlush(
                User.create(
                        "admin@email.com",
                        encoder.encode(PASSWORD),
                        UserRole.ADMIN
                )
        );

        String adminToken = login("admin@email.com");

        http.get()
                .uri("/api/admin/test-probe")
                .headers(headers -> headers.setBearerAuth(adminToken))
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void errorResponsesAndLogsDoNotContainSecrets(CapturedOutput output) {
        register("alice@email.com");
        String token = login("alice@email.com");

        String rejectedPassword = "Rejected-password-" + "x".repeat(80);

        assertSafeError(
                http.post()
                        .uri("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(Map.of(
                                "email", "other@email.com",
                                "password", rejectedPassword
                        ))
                        .exchange()
                        .expectStatus().isBadRequest(),
                PASSWORD, rejectedPassword, token
        );

        assertSafeError(
                http.post()
                        .uri("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(Map.of(
                                "email", "alice@email.com",
                                "password", PASSWORD
                        ))
                        .exchange()
                        .expectStatus().isEqualTo(409),
                PASSWORD, token
        );

        String wrongPassword = "Wrong-password-42";

        assertSafeError(
                http.post()
                        .uri("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(Map.of(
                                "email", "alice@email.com",
                                "password", wrongPassword
                        ))
                        .exchange()
                        .expectStatus().isUnauthorized(),
                PASSWORD, wrongPassword, token
        );

        String corruptedToken = token + "x";

        assertSafeError(
                http.get()
                        .uri("/api/users/me")
                        .headers(headers ->
                                headers.setBearerAuth(corruptedToken))
                        .exchange()
                        .expectStatus().isUnauthorized(),
                PASSWORD, token, corruptedToken
        );

        assertThat(output.getAll()).doesNotContain(
                PASSWORD,
                rejectedPassword,
                wrongPassword,
                token,
                corruptedToken
        );
    }

    private UserResult register(String email) {
        UserResult result = http.post()
                .uri("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("email", email, "password", PASSWORD))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(UserResult.class)
                .returnResult()
                .getResponseBody();

        assertThat(result).isNotNull();
        assertThat(result.id()).isNotNull();

        return result;
    }

    private String login(String email) {
        TokenResult result = http.post()
                .uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("email", email, "password", PASSWORD))
                .exchange()
                .expectStatus().isOk()
                .expectBody(TokenResult.class)
                .returnResult()
                .getResponseBody();

        assertThat(result).isNotNull();
        assertThat(result.type()).isEqualTo("Bearer");
        assertThat(result.token()).isNotBlank();

        return result.token();
    }

    private static void assertSafeError(
            RestTestClient.ResponseSpec response,
            String... secrets
    ) {
        var result = response.expectBody().returnResult();

        assertThat(
                body(result.getResponseBody())
                        + result.getResponseHeaders()
        ).doesNotContain(secrets);
    }

    private static String body(byte[] bytes) {
        return bytes == null
                ? ""
                : new String(bytes, StandardCharsets.UTF_8);
    }

    @TestConfiguration(proxyBeanMethods = false)
    @RestController
    static class AdminProbe {

        @GetMapping("/api/admin/test-probe")
        String probe() {
            return "ok";
        }
    }
}