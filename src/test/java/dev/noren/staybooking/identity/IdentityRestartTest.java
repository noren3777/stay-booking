package dev.noren.staybooking.identity;

import dev.noren.staybooking.StayBookingApplication;
import dev.noren.staybooking.identity.internal.application.service.AuthService;
import dev.noren.staybooking.identity.internal.domain.enums.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class IdentityRestartTest {

    @Container
    static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer("postgres:18.6-bookworm")
                    .withDatabaseName("identity_restart_test")
                    .withUsername("test")
                    .withPassword("test");

    @Test
    void persistedUserCanLoginAfterRestart() {
        String[] arguments = {
                "--server.port=0",
                "--spring.profiles.active=test",
                "--spring.datasource.url=" + POSTGRES.getJdbcUrl(),
                "--spring.datasource.username=" + POSTGRES.getUsername(),
                "--spring.datasource.password=" + POSTGRES.getPassword(),
                "--app.jwt.secret-base64=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
                "--app.jwt.issuer=identity-restart-test",
                "--app.jwt.ttl=PT1H"
        };

        String email = "restart@email.com";
        String password = "Integration-password-31";
        UUID userId;
        String oldToken;

        try (var first = new SpringApplicationBuilder(
                StayBookingApplication.class
        ).run(arguments)) {
            AuthService auth = first.getBean(AuthService.class);

            userId = auth.register(email, password, UserRole.GUEST).id();
            oldToken = auth.login(email, password).token();
        }

        try (var second = new SpringApplicationBuilder(
                StayBookingApplication.class
        ).run(arguments)) {
            AuthService auth = second.getBean(AuthService.class);
            JwtDecoder decoder = second.getBean(JwtDecoder.class);

            String newToken = auth.login(email, password).token();

            assertThat(decoder.decode(newToken).getSubject())
                    .isEqualTo(userId.toString());

            assertThat(decoder.decode(oldToken).getSubject())
                    .isEqualTo(userId.toString());
        }
    }
}