package dev.noren.staybooking;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpStatus;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.Duration;

import static org.awaitility.Awaitility.await;

@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@AutoConfigureRestTestClient
@RequiredArgsConstructor
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.datasource.hikari.connection-timeout=500"
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
        assertHealth(HttpStatus.OK, "UP");

        POSTGRES.stop();

        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> assertHealth(HttpStatus.SERVICE_UNAVAILABLE, "DOWN"));
    }

    private void assertHealth(HttpStatus expectedHttpStatus, String expectedHealth) {
        restTestClient.get()
                .uri("/actuator/health")
                .exchange()
                .expectStatus().isEqualTo(expectedHttpStatus)
                .expectBody()
                .jsonPath("$.status").isEqualTo(expectedHealth);
    }
}
