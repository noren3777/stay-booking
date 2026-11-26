package dev.noren.staybooking;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.Set;
import java.util.Map;
import java.util.List;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@AutoConfigureRestTestClient
@RequiredArgsConstructor
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "app.jwt.secret-base64=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=")
class OpenApiIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18.6-bookworm")
            .withDatabaseName("openapi_test")
            .withUsername("test")
            .withPassword("test");

    private final RestTestClient http;

    @TestConfiguration(proxyBeanMethods = false)
    static class FixedClockConfiguration {
        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(Instant.parse("2026-01-10T12:00:00Z"), ZoneOffset.UTC);
        }
    }

    @Test
    void publicDocumentationAndApiDoNotOpenProtectedRoutes() {
        http.get().uri("/swagger-ui.html").exchange().expectStatus().is3xxRedirection();
        http.get().uri("/swagger-ui/index.html").exchange().expectStatus().isOk();
        http.get().uri("/v3/api-docs").exchange().expectStatus().isOk();
        http.get().uri("/v3/api-docs.yaml").exchange().expectStatus().isOk();
        http.get().uri("/v3/api-docs/swagger-config").exchange().expectStatus().isOk();

        http.post().uri("/api/bookings").exchange().expectStatus().isUnauthorized();
        http.get().uri("/api/users/me").exchange().expectStatus().isUnauthorized();
        http.get().uri("/actuator/health").exchange().expectStatus().isUnauthorized();
        http.post().uri("/v3/api-docs").exchange().expectStatus().isUnauthorized();
        http.get().uri("/api/amenities").exchange().expectStatus().isOk();
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/amenities", "/api/properties/5d876d36-95d7-4d10-8181-ef5b699d625b"})
    void publicReadsRejectInvalidBearerTokens(String path) {
        http.get().uri(path)
                .headers(headers -> headers.setBearerAuth("not-a-jwt"))
                .exchange().expectStatus().isUnauthorized()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody().jsonPath("$.code").isEqualTo("UNAUTHORIZED");
    }

    @Test
    void generatedSpecMatchesImplementedRoutesSecurityAndErrors() {
        String json = http.get().uri("/v3/api-docs").exchange().expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody(String.class).returnResult().getResponseBody();
        assertThat(json).isNotNull();
        DocumentContext spec = JsonPath.parse(json);

        assertThat(keysAt(spec, "$.paths")).containsExactlyInAnyOrder(
                "/api/auth/register", "/api/auth/login", "/api/users/me",
                "/api/amenities", "/api/properties", "/api/properties/{id}",
                "/api/properties/{id}/management", "/api/properties/{id}/amenities",
                "/api/properties/{id}/images", "/api/properties/{id}/activate",
                "/api/properties/{id}/deactivate", "/api/properties/{id}/block",
                "/api/properties/{id}/unblock", "/api/properties/{id}/host",
                "/api/bookings");
        Map<String, List<String>> methods = Map.ofEntries(
                Map.entry("/api/auth/register", List.of("post")),
                Map.entry("/api/auth/login", List.of("post")),
                Map.entry("/api/users/me", List.of("get")),
                Map.entry("/api/amenities", List.of("get", "post")),
                Map.entry("/api/properties", List.of("get", "post")),
                Map.entry("/api/properties/{id}", List.of("get", "patch", "delete")),
                Map.entry("/api/properties/{id}/management", List.of("get")),
                Map.entry("/api/properties/{id}/amenities", List.of("put")),
                Map.entry("/api/properties/{id}/images", List.of("put")),
                Map.entry("/api/properties/{id}/activate", List.of("patch")),
                Map.entry("/api/properties/{id}/deactivate", List.of("patch")),
                Map.entry("/api/properties/{id}/block", List.of("patch")),
                Map.entry("/api/properties/{id}/unblock", List.of("patch")),
                Map.entry("/api/properties/{id}/host", List.of("patch")),
                Map.entry("/api/bookings", List.of("post")));
        methods.forEach((path, expected) -> assertThat(keysAt(spec, "$.paths['" + path + "']"))
                .as(path).containsExactlyInAnyOrderElementsOf(expected));
        assertThat(spec.read("$.components.securitySchemes.bearerAuth.scheme", String.class)).isEqualTo("bearer");
        Set<String> publicOperations = Set.of(
                "post /api/auth/register", "post /api/auth/login", "get /api/amenities", "get /api/properties/{id}");
        methods.forEach((path, verbs) -> verbs.forEach(verb -> {
            Map<String, Object> operation = spec.read("$.paths['" + path + "']." + verb);
            String route = verb + " " + path;
            if (publicOperations.contains(route)) {
                assertThat(operation).as(route).doesNotContainKey("security");
            } else if (route.equals("get /api/properties")) {
                assertThat(operation.get("security")).as(route)
                        .isEqualTo(List.of(Map.of("bearerAuth", List.of()), Map.of()));
            } else {
                List<?> scopes = JsonPath.read(operation, "$.security[0].bearerAuth");
                assertThat(scopes).as(route).isEmpty();
            }
        }));
        List<Map<String, Object>> catalogParameters = spec.read("$.paths['/api/properties'].get.parameters");
        assertThat(catalogParameters)
                .extracting(parameter -> parameter.get("name"))
                .containsExactlyInAnyOrder("sort", "city", "minPrice", "maxPrice", "minGuests", "status", "page", "size");
        Map<String, Object> sort = catalogParameters.stream().filter(parameter -> "sort".equals(parameter.get("name")))
                .findFirst().orElseThrow();
        String sortItemType = JsonPath.read(sort, "$.schema.items.type");
        assertThat(sortItemType).isEqualTo("string");
        assertThat(spec.read("$.paths['/api/properties/{id}'].patch.parameters[1].required", Boolean.class)).isTrue();
        for (String path : List.of("/api/properties/{id}", "/api/properties/{id}/management")) {
            String operation = "$.paths['" + path + "'].get";
            List<Map<String, Object>> headers = spec.read(operation + ".parameters[?(@.name == 'If-None-Match')]");
            assertThat(headers).singleElement().satisfies(header -> assertThat(header)
                    .containsEntry("in", "header")
                    .doesNotContainEntry("required", true));
            String notModified = operation + ".responses['304']";
            assertThat(spec.<Map<String, Object>>read(notModified)).doesNotContainKey("content");
            assertThat(spec.read(notModified + ".headers.ETag.schema.type", String.class)).isEqualTo("string");
        }
        for (String path : List.of("/api/amenities", "/api/properties/{id}")) {
            String unauthorized = "$.paths['" + path + "'].get.responses['401']";
            assertThat(spec.read(unauthorized + ".content['application/problem+json'].schema['$ref']", String.class))
                    .isEqualTo("#/components/schemas/ApiProblem");
        }
        assertThat(spec.read("$.paths['/api/amenities'].get.responses['200'].content['application/json'].schema.items['$ref']", String.class))
                .isEqualTo("#/components/schemas/AmenityResponse");
        assertThat(spec.read("$.paths['/api/bookings'].post.requestBody.content['application/json'].schema['$ref']", String.class))
                .isEqualTo("#/components/schemas/CreateBookingRequest");
        assertThat(spec.read("$.paths['/api/bookings'].post.responses['201'].content['application/json'].schema['$ref']", String.class))
                .isEqualTo("#/components/schemas/BookingResponse");
        assertThat(spec.read("$.paths['/api/properties'].get.responses['200'].content['application/json'].schema['$ref']", String.class))
                .isEqualTo("#/components/schemas/PropertyCatalogResponse");

        assertThat(keysAt(spec, "$.paths['/api/bookings'].post.responses"))
                .contains("201", "400", "401", "403", "404", "409", "500");
        assertThat(keysAt(spec, "$.paths['/api/properties/{id}'].patch.responses"))
                .contains("200", "412", "428");
        for (String segment : List.of("amenities", "images")) {
            String responses = "$.paths['/api/properties/{id}/" + segment + "'].put.responses";
            assertThat(keysAt(spec, responses)).contains("204", "409", "412", "428");
            assertThat(spec.read(responses + "['409'].content['application/problem+json'].schema['$ref']", String.class))
                    .isEqualTo("#/components/schemas/ApiProblem");
        }
        assertThat(spec.<Map<String, Object>>read("$.paths['/api/properties/{id}/images'].put.responses['204']"))
                .doesNotContainKey("content");
        assertThat(spec.read("$.paths['/api/properties/{id}/host'].patch.responses['404'].description", String.class))
                .isEqualTo("Property not found");
        assertThat(keysAt(spec, "$.paths['/api/properties/{id}/host'].patch.responses"))
                .contains("400")
                .doesNotContain("409");
        assertThat(keysAt(spec, "$.paths['/api/properties'].post.responses['201'].headers"))
                .containsExactlyInAnyOrder(HttpHeaders.ETAG, HttpHeaders.LOCATION);
        assertThat(spec.read("$.paths['/api/properties/{id}'].patch.responses['200'].headers.ETag.schema.type", String.class))
                .isEqualTo("string");
        assertThat(spec.read("$.paths['/api/properties/{id}/activate'].patch.responses['204'].headers.ETag.required", Boolean.class))
                .isTrue();
        assertThat(spec.read("$.components.schemas.UpdatePropertyRequest.properties.maxGuests.type", String.class))
                .isEqualTo("integer");
        assertThat(spec.<Map<String, Object>>read("$.components.schemas.UpdatePropertyRequest")).doesNotContainKey("required");
        assertThat(spec.read("$.paths['/api/bookings'].post.responses['400'].content['application/problem+json'].schema['$ref']", String.class))
                .isEqualTo("#/components/schemas/ApiProblem");
        assertThat(spec.read("$.paths['/api/bookings'].post.responses['500'].content['application/problem+json'].schema['$ref']", String.class))
                .isEqualTo("#/components/schemas/ApiProblem");
        assertThat(keysAt(spec, "$.components.schemas.ApiProblem.properties"))
                .containsAll(Set.of("type", "title", "status", "detail", "code", "path", "requestId", "errors"));
        assertThat(spec.<List<String>>read("$.components.schemas.BookingResponse.properties.status.enum"))
                .containsExactly("PENDING_PAYMENT");
        assertThat(spec.<List<String>>read("$.components.schemas.RegisterRequest.properties.role.enum"))
                .containsExactly("GUEST", "HOST");
    }

    @Test
    void documentedExamplesPassActualHttpFlow() {
        String guestToken = registerAndLogin("alice@example.com", "GUEST");
        String hostToken = registerAndLogin("host@example.com", "HOST");

        var created = http.post().uri("/api/properties")
                .headers(headers -> headers.setBearerAuth(hostToken))
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "title", "Quiet apartment", "country", "Ukraine", "city", "Kyiv",
                        "address", "Khreshchatyk 1", "pricePerNight", 120.00, "maxGuests", 4))
                .exchange().expectStatus().isCreated()
                .expectBody(Map.class).returnResult();
        Map<?, ?> property = created.getResponseBody();
        assertThat(property).isNotNull();
        String propertyId = (String) property.get("id");
        String etag = created.getResponseHeaders().getETag();
        assertThat(etag).isNotNull();
        assertThat(created.getResponseHeaders().getLocation()).hasToString("/api/properties/" + propertyId);

        http.patch().uri("/api/properties/{id}/activate", propertyId)
                .headers(headers -> {
                    headers.setBearerAuth(hostToken);
                    headers.set(HttpHeaders.IF_MATCH, etag);
                }).exchange().expectStatus().isNoContent()
                .expectHeader().exists(HttpHeaders.ETAG);

        var active = http.get().uri("/api/properties/{id}", propertyId)
                .header(HttpHeaders.IF_NONE_MATCH, etag)
                .exchange().expectStatus().isOk()
                .expectBody().jsonPath("$.id").isEqualTo(propertyId).returnResult();
        String activeEtag = active.getResponseHeaders().getETag();
        assertThat(activeEtag).isNotBlank().isNotEqualTo(etag);

        http.get().uri("/api/properties/{id}", propertyId)
                .header(HttpHeaders.IF_NONE_MATCH, activeEtag)
                .exchange().expectStatus().isNotModified()
                .expectHeader().valueEquals(HttpHeaders.ETAG, activeEtag)
                .expectBody().isEmpty();
        http.get().uri("/api/properties/{id}/management", propertyId)
                .headers(headers -> headers.setBearerAuth(hostToken))
                .header(HttpHeaders.IF_NONE_MATCH, activeEtag)
                .exchange().expectStatus().isNotModified()
                .expectHeader().valueEquals(HttpHeaders.ETAG, activeEtag)
                .expectBody().isEmpty();

        http.post().uri("/api/bookings")
                .headers(headers -> headers.setBearerAuth(guestToken))
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("propertyId", propertyId, "checkIn", "2030-06-10",
                        "checkOut", "2030-06-12", "numberOfGuests", 2))
                .exchange().expectStatus().isCreated()
                .expectBody().jsonPath("$.status").isEqualTo("PENDING_PAYMENT")
                .jsonPath("$.totalPrice").isEqualTo(240.0);
    }

    private static Set<String> keysAt(DocumentContext spec, String path) {
        return spec.<Map<String, Object>>read(path).keySet();
    }

    private String registerAndLogin(String email, String role) {
        http.post().uri("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("email", email, "password", "Integration-password-31", "role", role))
                .exchange().expectStatus().isCreated();

        Map<?, ?> login = http.post().uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("email", email, "password", "Integration-password-31"))
                .exchange().expectStatus().isOk().expectBody(Map.class).returnResult().getResponseBody();
        assertThat(login).isNotNull();
        return (String) login.get("token");
    }
}
