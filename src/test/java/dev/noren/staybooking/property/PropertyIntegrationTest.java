package dev.noren.staybooking.property;

import dev.noren.staybooking.identity.internal.application.result.TokenResult;
import dev.noren.staybooking.identity.internal.domain.entity.User;
import dev.noren.staybooking.identity.internal.domain.enums.UserRole;
import dev.noren.staybooking.identity.internal.persistence.UserRepository;
import dev.noren.staybooking.property.internal.domain.enums.PropertyStatus;
import dev.noren.staybooking.property.internal.web.response.PropertyResponse;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@AutoConfigureRestTestClient
@RequiredArgsConstructor
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "app.jwt.secret-base64=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
                "app.jwt.issuer=property-integration-test",
                "app.jwt.ttl=PT1H"
        }
)
class PropertyIntegrationTest {

    private static final String PASSWORD = "Integration-password-31";

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer("postgres:18.6-bookworm")
                    .withDatabaseName("property_integration_test")
                    .withUsername("test")
                    .withPassword("test");

    private final RestTestClient http;
    private final UserRepository users;
    private final PasswordEncoder encoder;

    @Test
    void inactiveListingIsPrivateUntilActivatedAndHiddenAfterDeletion() {
        String hostToken = createUserAndLogin(UserRole.HOST);
        CreatedProperty created = createProperty(hostToken);

        assertThat(created.response().status()).isEqualTo(PropertyStatus.INACTIVE);
        assertThat(created.response().version()).isEqualTo(0L);
        assertThat(created.etag()).isEqualTo("\"0\"");

        http.get()
                .uri("/api/properties/{id}", created.id())
                .exchange()
                .expectStatus().isNotFound();

        http.get()
                .uri("/api/properties/{id}/management", created.id())
                .headers(headers -> headers.setBearerAuth(hostToken))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.title").isEqualTo("Flat near the park");

        String activeEtag = http.patch()
                .uri("/api/properties/{id}/activate", created.id())
                .headers(headers -> {
                    headers.setBearerAuth(hostToken);
                    headers.set(HttpHeaders.IF_MATCH, created.etag());
                })
                .exchange()
                .expectStatus().isNoContent()
                .returnResult(String.class)
                .getResponseHeaders()
                .getETag();

        assertThat(activeEtag).isNotBlank().isNotEqualTo(created.etag());

        http.get()
                .uri("/api/properties/{id}", created.id())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("ACTIVE");

        http.delete()
                .uri("/api/properties/{id}", created.id())
                .headers(headers -> {
                    headers.setBearerAuth(hostToken);
                    headers.set(HttpHeaders.IF_MATCH, activeEtag);
                })
                .exchange()
                .expectStatus().isNoContent();

        http.get()
                .uri("/api/properties/{id}", created.id())
                .exchange()
                .expectStatus().isNotFound();

        http.get()
                .uri("/api/properties/{id}/management", created.id())
                .headers(headers -> headers.setBearerAuth(hostToken))
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void adminCanBlockAndReassignWhileHostsKeepTheirOwnAccess() {
        String ownerToken = createUserAndLogin(UserRole.HOST);
        User newHost = createUser(UserRole.HOST);
        String otherHostToken = login(newHost);
        String adminToken = createUserAndLogin(UserRole.ADMIN);
        CreatedProperty created = createProperty(ownerToken);
        UUID id = created.id();
        String etag = created.etag();

        http.get()
                .uri("/api/properties/{id}/management", id)
                .headers(headers -> headers.setBearerAuth(otherHostToken))
                .exchange()
                .expectStatus().isForbidden();

        http.patch()
                .uri("/api/properties/{id}", id)
                .headers(headers -> {
                    headers.setBearerAuth(otherHostToken);
                    headers.set(HttpHeaders.IF_MATCH, etag);
                })
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("title", "Wrong owner's edit"))
                .exchange()
                .expectStatus().isForbidden();

        http.patch()
                .uri("/api/properties/{id}/block", id)
                .headers(headers -> {
                    headers.setBearerAuth(ownerToken);
                    headers.set(HttpHeaders.IF_MATCH, etag);
                })
                .exchange()
                .expectStatus().isForbidden();

        String blockedEtag = http.patch()
                .uri("/api/properties/{id}/block", id)
                .headers(headers -> {
                    headers.setBearerAuth(adminToken);
                    headers.set(HttpHeaders.IF_MATCH, etag);
                })
                .exchange()
                .expectStatus().isNoContent()
                .returnResult(String.class)
                .getResponseHeaders()
                .getETag();

        assertThat(blockedEtag).isNotBlank().isNotEqualTo(etag);

        http.patch()
                .uri("/api/properties/{id}/unblock", id)
                .headers(headers -> {
                    headers.setBearerAuth(ownerToken);
                    headers.set(HttpHeaders.IF_MATCH, blockedEtag);
                })
                .exchange()
                .expectStatus().isForbidden();

        String unblockedEtag = http.patch()
                .uri("/api/properties/{id}/unblock", id)
                .headers(headers -> {
                    headers.setBearerAuth(adminToken);
                    headers.set(HttpHeaders.IF_MATCH, blockedEtag);
                })
                .exchange()
                .expectStatus().isNoContent()
                .returnResult(String.class)
                .getResponseHeaders()
                .getETag();

        assertThat(unblockedEtag).isNotBlank().isNotEqualTo(blockedEtag);

        String reassignedEtag = http.patch()
                .uri("/api/properties/{id}/host", id)
                .headers(headers -> {
                    headers.setBearerAuth(adminToken);
                    headers.set(HttpHeaders.IF_MATCH, unblockedEtag);
                })
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("hostId", newHost.getId().toString()))
                .exchange()
                .expectStatus().isNoContent()
                .returnResult(String.class)
                .getResponseHeaders()
                .getETag();

        assertThat(reassignedEtag).isNotBlank().isNotEqualTo(unblockedEtag);

        http.get()
                .uri("/api/properties/{id}/management", id)
                .headers(headers -> headers.setBearerAuth(ownerToken))
                .exchange()
                .expectStatus().isForbidden();

        http.get()
                .uri("/api/properties/{id}/management", id)
                .headers(headers -> headers.setBearerAuth(otherHostToken))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.hostId").isEqualTo(newHost.getId().toString())
                .jsonPath("$.status").isEqualTo("INACTIVE")
                .jsonPath("$.title").isEqualTo("Flat near the park");
    }

    @Test
    void adminCanReassignBlockedPropertyWithoutUnblockingIt() {
        String ownerToken = createUserAndLogin(UserRole.HOST);
        User newHost = createUser(UserRole.HOST);
        String adminToken = createUserAndLogin(UserRole.ADMIN);
        CreatedProperty created = createProperty(ownerToken);

        String blockedEtag = http.patch()
                .uri("/api/properties/{id}/block", created.id())
                .headers(headers -> {
                    headers.setBearerAuth(adminToken);
                    headers.set(HttpHeaders.IF_MATCH, created.etag());
                })
                .exchange()
                .expectStatus().isNoContent()
                .returnResult(String.class)
                .getResponseHeaders()
                .getETag();

        http.patch()
                .uri("/api/properties/{id}/host", created.id())
                .headers(headers -> {
                    headers.setBearerAuth(ownerToken);
                    headers.set(HttpHeaders.IF_MATCH, blockedEtag);
                })
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("hostId", newHost.getId().toString()))
                .exchange()
                .expectStatus().isForbidden();

        http.patch()
                .uri("/api/properties/{id}/host", created.id())
                .headers(headers -> {
                    headers.setBearerAuth(adminToken);
                    headers.set(HttpHeaders.IF_MATCH, blockedEtag);
                })
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("hostId", newHost.getId().toString()))
                .exchange()
                .expectStatus().isNoContent();

        var current = http.get()
                .uri("/api/properties/{id}/management", created.id())
                .headers(headers -> headers.setBearerAuth(adminToken))
                .exchange()
                .expectStatus().isOk()
                .expectBody(PropertyResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(current).isNotNull();
        assertThat(current.hostId()).isEqualTo(newHost.getId());
        assertThat(current.status()).isEqualTo(PropertyStatus.BLOCKED);
    }

    @Test
    void adminCannotAssignPropertyToGuestOrUnknownUser() {
        String ownerToken = createUserAndLogin(UserRole.HOST);
        User guest = createUser(UserRole.GUEST);
        String adminToken = createUserAndLogin(UserRole.ADMIN);
        CreatedProperty created = createProperty(ownerToken);

        for (UUID invalidHostId : List.of(guest.getId(), UUID.randomUUID())) {
            http.patch()
                    .uri("/api/properties/{id}/host", created.id())
                    .headers(headers -> {
                        headers.setBearerAuth(adminToken);
                        headers.set(HttpHeaders.IF_MATCH, created.etag());
                    })
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("hostId", invalidHostId.toString()))
                    .exchange()
                    .expectStatus().isBadRequest();
        }

        var current = http.get()
                .uri("/api/properties/{id}/management", created.id())
                .headers(headers -> headers.setBearerAuth(adminToken))
                .exchange()
                .expectStatus().isOk()
                .expectBody(PropertyResponse.class)
                .returnResult();

        assertThat(current.getResponseBody()).isNotNull();
        assertThat(current.getResponseBody().hostId()).isEqualTo(created.response().hostId());
        assertThat(current.getResponseBody().version()).isEqualTo(created.response().version());
        assertThat(current.getResponseHeaders().getETag()).isEqualTo(created.etag());
    }

    @Test
    void updateRequiresTheLatestStrongEtag() {
        String hostToken = createUserAndLogin(UserRole.HOST);
        CreatedProperty created = createProperty(hostToken);

        http.patch()
                .uri("/api/properties/{id}", created.id())
                .headers(headers -> headers.setBearerAuth(hostToken))
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("title", "Flat with balcony"))
                .exchange()
                .expectStatus().isEqualTo(428);

        http.patch()
                .uri("/api/properties/{id}", created.id())
                .headers(headers -> {
                    headers.setBearerAuth(hostToken);
                    headers.set(HttpHeaders.IF_MATCH, "W/" + created.etag());
                })
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("title", "Flat with balcony"))
                .exchange()
                .expectStatus().isBadRequest();

        var updated = http.patch()
                .uri("/api/properties/{id}", created.id())
                .headers(headers -> {
                    headers.setBearerAuth(hostToken);
                    headers.set(HttpHeaders.IF_MATCH, created.etag());
                })
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("title", "Flat with balcony"))
                .exchange()
                .expectStatus().isOk()
                .expectBody(PropertyResponse.class)
                .returnResult();

        assertThat(updated.getResponseBody()).isNotNull();
        assertThat(updated.getResponseBody().title()).isEqualTo("Flat with balcony");
        assertThat(updated.getResponseBody().version()).isEqualTo(1L);
        assertThat(updated.getResponseHeaders().getETag()).isEqualTo("\"1\"");

        http.patch()
                .uri("/api/properties/{id}", created.id())
                .headers(headers -> {
                    headers.setBearerAuth(hostToken);
                    headers.set(HttpHeaders.IF_MATCH, created.etag());
                })
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("title", "Outdated edit"))
                .exchange()
                .expectStatus().isEqualTo(412);

        http.get()
                .uri("/api/properties/{id}/management", created.id())
                .headers(headers -> headers.setBearerAuth(hostToken))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.title").isEqualTo("Flat with balcony");
    }

    @Test
    void noOpPatchKeepsVersionAndEtag() {
        String hostToken = createUserAndLogin(UserRole.HOST);
        CreatedProperty created = createProperty(hostToken);

        for (String patch : List.of("{}", "{\"title\":\"Flat near the park\"}")) {
            var result = http.patch()
                    .uri("/api/properties/{id}", created.id())
                    .headers(headers -> {
                        headers.setBearerAuth(hostToken);
                        headers.set(HttpHeaders.IF_MATCH, created.etag());
                    })
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(patch)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(PropertyResponse.class)
                    .returnResult();

            assertThat(result.getResponseBody()).isNotNull();
            assertThat(result.getResponseBody().version()).isEqualTo(created.response().version());
            assertThat(result.getResponseBody().title()).isEqualTo(created.response().title());
            assertThat(result.getResponseHeaders().getETag()).isEqualTo(created.etag());
        }

        var current = http.get()
                .uri("/api/properties/{id}/management", created.id())
                .headers(headers -> headers.setBearerAuth(hostToken))
                .exchange()
                .expectStatus().isOk()
                .expectBody(PropertyResponse.class)
                .returnResult();

        assertThat(current.getResponseBody()).isNotNull();
        assertThat(current.getResponseBody().version()).isEqualTo(created.response().version());
        assertThat(current.getResponseHeaders().getETag()).isEqualTo(created.etag());
    }

    @Test
    void patchDistinguishesExplicitNullFromOmittedField() {
        String hostToken = createUserAndLogin(UserRole.HOST);
        CreatedProperty created = createProperty(hostToken);

        var cleared = http.patch()
                .uri("/api/properties/{id}", created.id())
                .headers(headers -> {
                    headers.setBearerAuth(hostToken);
                    headers.set(HttpHeaders.IF_MATCH, created.etag());
                })
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"description\":null}")
                .exchange()
                .expectStatus().isOk()
                .expectBody(PropertyResponse.class)
                .returnResult();

        assertThat(cleared.getResponseBody()).isNotNull();
        assertThat(cleared.getResponseBody().description()).isNull();
        assertThat(cleared.getResponseBody().title()).isEqualTo(created.response().title());

        var renamed = http.patch()
                .uri("/api/properties/{id}", created.id())
                .headers(headers -> {
                    headers.setBearerAuth(hostToken);
                    headers.set(HttpHeaders.IF_MATCH, cleared.getResponseHeaders().getETag());
                })
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("title", "Flat with balcony"))
                .exchange()
                .expectStatus().isOk()
                .expectBody(PropertyResponse.class)
                .returnResult();

        assertThat(renamed.getResponseBody()).isNotNull();
        assertThat(renamed.getResponseBody().title()).isEqualTo("Flat with balcony");
        assertThat(renamed.getResponseBody().description()).isNull();
    }

    @Test
    void invalidPatchDoesNotPersistAnyField() {
        String hostToken = createUserAndLogin(UserRole.HOST);
        CreatedProperty created = createProperty(hostToken);

        http.patch()
                .uri("/api/properties/{id}", created.id())
                .headers(headers -> {
                    headers.setBearerAuth(hostToken);
                    headers.set(HttpHeaders.IF_MATCH, created.etag());
                })
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("title", "Changed title", "pricePerNight", 0))
                .exchange()
                .expectStatus().isBadRequest();

        var unchanged = http.get()
                .uri("/api/properties/{id}/management", created.id())
                .headers(headers -> headers.setBearerAuth(hostToken))
                .exchange()
                .expectStatus().isOk()
                .expectBody(PropertyResponse.class)
                .returnResult();

        assertThat(unchanged.getResponseBody()).isNotNull();
        assertThat(unchanged.getResponseBody().title()).isEqualTo(created.response().title());
        assertThat(unchanged.getResponseBody().pricePerNight())
                .isEqualByComparingTo(created.response().pricePerNight());
        assertThat(unchanged.getResponseBody().version()).isEqualTo(created.response().version());
    }

    @Test
    void anotherHostCannotActivateOrDeleteProperty() {
        String ownerToken = createUserAndLogin(UserRole.HOST);
        String otherHostToken = createUserAndLogin(UserRole.HOST);
        CreatedProperty created = createProperty(ownerToken);

        http.patch()
                .uri("/api/properties/{id}/activate", created.id())
                .headers(headers -> {
                    headers.setBearerAuth(otherHostToken);
                    headers.set(HttpHeaders.IF_MATCH, created.etag());
                })
                .exchange()
                .expectStatus().isForbidden();

        http.delete()
                .uri("/api/properties/{id}", created.id())
                .headers(headers -> {
                    headers.setBearerAuth(otherHostToken);
                    headers.set(HttpHeaders.IF_MATCH, created.etag());
                })
                .exchange()
                .expectStatus().isForbidden();

        var unchanged = http.get()
                .uri("/api/properties/{id}/management", created.id())
                .headers(headers -> headers.setBearerAuth(ownerToken))
                .exchange()
                .expectStatus().isOk()
                .expectBody(PropertyResponse.class)
                .returnResult();

        assertThat(unchanged.getResponseBody()).isNotNull();
        assertThat(unchanged.getResponseBody().status()).isEqualTo(PropertyStatus.INACTIVE);
        assertThat(unchanged.getResponseBody().version()).isEqualTo(created.response().version());
    }

    @Test
    void anonymousAndGuestCannotCreateProperty() {
        http.post()
                .uri("/api/properties")
                .contentType(MediaType.APPLICATION_JSON)
                .body(createRequest())
                .exchange()
                .expectStatus().isUnauthorized();

        String guestToken = createUserAndLogin(UserRole.GUEST);

        http.post()
                .uri("/api/properties")
                .headers(headers -> headers.setBearerAuth(guestToken))
                .contentType(MediaType.APPLICATION_JSON)
                .body(createRequest())
                .exchange()
                .expectStatus().isForbidden();
    }

    private CreatedProperty createProperty(String hostToken) {
        var result = http.post()
                .uri("/api/properties")
                .headers(headers -> headers.setBearerAuth(hostToken))
                .contentType(MediaType.APPLICATION_JSON)
                .body(createRequest())
                .exchange()
                .expectStatus().isCreated()
                .expectBody(PropertyResponse.class)
                .returnResult();

        PropertyResponse body = result.getResponseBody();
        assertThat(body).isNotNull();
        assertThat(body.id()).isNotNull();
        assertThat(result.getResponseHeaders().getLocation())
                .hasPath("/api/properties/" + body.id());

        return new CreatedProperty(
                body.id(), result.getResponseHeaders().getETag(), body
        );
    }

    private String createUserAndLogin(UserRole role) {
        return login(createUser(role));
    }

    private User createUser(UserRole role) {
        String email = role.name().toLowerCase() + "-" + UUID.randomUUID() + "@example.com";
        return users.saveAndFlush(User.create(email, encoder.encode(PASSWORD), role));
    }

    private String login(User user) {
        TokenResult result = http.post()
                .uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("email", user.getEmail(), "password", PASSWORD))
                .exchange()
                .expectStatus().isOk()
                .expectBody(TokenResult.class)
                .returnResult()
                .getResponseBody();

        assertThat(result).isNotNull();
        assertThat(result.token()).isNotBlank();
        return result.token();
    }

    private static Map<String, Object> createRequest() {
        return Map.of(
                "title", "Flat near the park",
                "description", "Second floor, balcony",
                "country", "Ukraine",
                "city", "Kyiv",
                "address", "14 Khreshchatyk Street",
                "pricePerNight", 125.50,
                "maxGuests", 3
        );
    }

    private record CreatedProperty(UUID id, String etag, PropertyResponse response) {
    }
}
