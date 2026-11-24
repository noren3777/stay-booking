package dev.noren.staybooking;

import dev.noren.staybooking.identity.internal.web.response.TokenResponse;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@AutoConfigureRestTestClient
@RequiredArgsConstructor
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.datasource.hikari.connection-timeout=500",
                "app.jwt.secret-base64=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
                "app.jwt.ttl=PT1H"
        }
)
class DatabaseHealthFailureTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer("postgres:18.6-bookworm")
                    .withDatabaseName("staybooking_health_test")
                    .withUsername("test")
                    .withPassword("test");

    private final RestTestClient restTestClient;

    @Test
    void healthBecomesDownWhenDatabaseIsUnavailable() {
        String token = registerAndLogin();

        assertHealth(token, HttpStatus.OK, "UP");

        POSTGRES.stop();

        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(() ->
                        assertHealth(
                                token,
                                HttpStatus.SERVICE_UNAVAILABLE,
                                "DOWN"
                        ));
    }

    private void assertHealth(
            String token,
            HttpStatus expectedHttpStatus,
            String expectedHealth
    ) {
        restTestClient.get()
                .uri("/actuator/health")
                .headers(headers -> headers.setBearerAuth(token))
                .exchange()
                .expectStatus().isEqualTo(expectedHttpStatus)
                .expectBody()
                .jsonPath("$.status").isEqualTo(expectedHealth);
    }

    private String registerAndLogin() {
        var credentials = Map.of(
                "email", "health-" + UUID.randomUUID() + "@example.com",
                "password", "Test-password-42"
        );

        restTestClient.post()
                .uri("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body(credentials)
                .exchange()
                .expectStatus().isCreated();

        TokenResponse result = restTestClient.post()
                .uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(credentials)
                .exchange()
                .expectStatus().isOk()
                .expectBody(TokenResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(result).isNotNull();
        assertThat(result.token()).isNotBlank();

        return result.token();
    }
}
