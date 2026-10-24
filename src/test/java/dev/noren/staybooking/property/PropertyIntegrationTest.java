package dev.noren.staybooking.property;

import dev.noren.staybooking.bootstrap.exception.EntityVersionConflictException;
import dev.noren.staybooking.identity.internal.application.result.TokenResult;
import dev.noren.staybooking.identity.internal.domain.entity.User;
import dev.noren.staybooking.identity.internal.domain.enums.UserRole;
import dev.noren.staybooking.identity.internal.persistence.UserRepository;
import dev.noren.staybooking.property.internal.application.service.PropertyService;
import dev.noren.staybooking.property.internal.domain.entity.Amenity;
import dev.noren.staybooking.property.internal.domain.entity.Property;
import dev.noren.staybooking.property.internal.domain.enums.PropertyStatus;
import dev.noren.staybooking.property.internal.domain.input.PropertyImageDetails;
import dev.noren.staybooking.property.internal.persistence.repository.AmenityRepository;
import dev.noren.staybooking.property.internal.persistence.repository.PropertyRepository;
import dev.noren.staybooking.property.internal.web.response.AmenityResponse;
import dev.noren.staybooking.property.internal.web.response.PropertyImageResponse;
import dev.noren.staybooking.property.internal.web.response.PropertyResponse;
import jakarta.persistence.EntityManager;
import jakarta.persistence.OptimisticLockException;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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
    private final AmenityRepository amenities;
    private final PropertyRepository properties;
    private final PropertyService propertyService;
    private final JdbcTemplate jdbc;
    private final EntityManager entityManager;
    private final TransactionTemplate transactions;
    private final PasswordEncoder encoder;

    @Test
    void amenityEqualityWorksAcrossPersistenceContextsAndKeepsHashStable() {
        Amenity amenity = Amenity.create("Wi-Fi-" + UUID.randomUUID());
        Amenity anotherNewAmenity = Amenity.create(amenity.getName());
        assertThat(amenity).isNotEqualTo(anotherNewAmenity);

        Set<Amenity> selected = new HashSet<>();
        selected.add(amenity);
        int hashBeforeSave = amenity.hashCode();
        amenities.saveAndFlush(amenity);

        Amenity reloaded = amenities.findById(amenity.getId()).orElseThrow();
        assertThat(reloaded).isNotSameAs(amenity);
        assertThat(reloaded).isEqualTo(amenity);
        assertThat(reloaded.hashCode()).isEqualTo(hashBeforeSave);
        assertThat(selected.contains(reloaded)).isTrue();

        Amenity different = amenities.saveAndFlush(Amenity.create("Parking-" + UUID.randomUUID()));
        assertThat(different).isNotEqualTo(amenity);

        transactions.executeWithoutResult(status -> {
            Amenity reference = entityManager.getReference(Amenity.class, amenity.getId());
            assertThat(reference).isEqualTo(amenity);
            assertThat(amenity).isEqualTo(reference);
            assertThat(selected.contains(reference)).isTrue();
        });
    }

    @Test
    void amenitiesAreReplacedAndClearedWithoutChangingCatalog() {
        String hostToken = createUserAndLogin(UserRole.HOST);
        CreatedProperty created = createProperty(hostToken);
        String wifiName = "Wi-Fi-" + UUID.randomUUID();
        String parkingName = "Parking-" + UUID.randomUUID();
        Amenity wifi = amenities.saveAndFlush(Amenity.create(wifiName));
        Amenity parking = amenities.saveAndFlush(Amenity.create(parkingName));

        var assigned = putAmenities(hostToken, created.id(), created.etag(),
                List.of(wifi.getId(), parking.getId()))
                .expectStatus().isNoContent()
                .expectBody().isEmpty();

        assertThat(assigned.getResponseHeaders().getETag()).isEqualTo("\"1\"");
        assertThat(joinCount(created.id())).isEqualTo(2);

        http.get()
                .uri("/api/properties/{id}", created.id())
                .exchange().expectStatus().isNotFound();
        var managementDetail = http.get()
                .uri("/api/properties/{id}/management", created.id())
                .headers(headers -> headers.setBearerAuth(hostToken))
                .exchange().expectStatus().isOk()
                .expectBody(PropertyResponse.class).returnResult();
        assertThat(managementDetail.getResponseBody()).isNotNull();
        assertThat(managementDetail.getResponseBody().amenities())
                .extracting(AmenityResponse::name)
                .containsExactly(parkingName, wifiName);
        assertThat(managementDetail.getResponseBody().version()).isEqualTo(1L);
        assertThat(managementDetail.getResponseHeaders().getETag()).isEqualTo("\"1\"");

        var repeated = putAmenities(hostToken, created.id(), "\"1\"",
                List.of(parking.getId(), wifi.getId()))
                .expectStatus().isNoContent()
                .expectBody().isEmpty();
        assertThat(repeated.getResponseHeaders().getETag()).isEqualTo("\"1\"");

        var replaced = putAmenities(hostToken, created.id(), "\"1\"", List.of(wifi.getId()))
                .expectStatus().isNoContent()
                .expectBody().isEmpty();
        assertThat(replaced.getResponseHeaders().getETag()).isEqualTo("\"2\"");
        assertThat(joinCount(created.id())).isEqualTo(1);

        var cleared = putAmenities(hostToken, created.id(), "\"2\"", List.of())
                .expectStatus().isNoContent()
                .expectBody().isEmpty();
        assertThat(cleared.getResponseHeaders().getETag()).isEqualTo("\"3\"");
        assertThat(joinCount(created.id())).isZero();
        assertThat(amenities.existsById(wifi.getId())).isTrue();
        assertThat(amenities.existsById(parking.getId())).isTrue();

        var emptyDetail = http.get()
                .uri("/api/properties/{id}/management", created.id())
                .headers(headers -> headers.setBearerAuth(hostToken))
                .exchange().expectStatus().isOk()
                .expectBody(PropertyResponse.class).returnResult();
        assertThat(emptyDetail.getResponseBody()).isNotNull();
        assertThat(emptyDetail.getResponseBody().amenities()).isEmpty();
        assertThat(emptyDetail.getResponseHeaders().getETag()).isEqualTo("\"3\"");
    }

    @Test
    void amenityChangesRequireOwnerCurrentVersionAndValidIds() {
        String ownerToken = createUserAndLogin(UserRole.HOST);
        String otherHostToken = createUserAndLogin(UserRole.HOST);
        String adminToken = createUserAndLogin(UserRole.ADMIN);
        CreatedProperty created = createProperty(ownerToken);
        Amenity wifi = amenities.saveAndFlush(Amenity.create("Wi-Fi-" + UUID.randomUUID()));

        putAmenities(otherHostToken, created.id(), created.etag(), List.of(wifi.getId()))
                .expectStatus().isForbidden();
        putAmenities(ownerToken, created.id(), null, List.of(wifi.getId()))
                .expectStatus().isEqualTo(428);
        putAmenities(ownerToken, created.id(), created.etag(), List.of(wifi.getId(), UUID.randomUUID()))
                .expectStatus().isNotFound();
        putAmenities(ownerToken, created.id(), created.etag(), List.of(wifi.getId(), wifi.getId()))
                .expectStatus().isBadRequest();
        assertThat(joinCount(created.id())).isZero();

        putAmenities(ownerToken, created.id(), created.etag(), List.of(wifi.getId()))
                .expectStatus().isNoContent();
        putAmenities(ownerToken, created.id(), created.etag(), List.of())
                .expectStatus().isEqualTo(412);
        assertThat(joinCount(created.id())).isEqualTo(1);

        String blockedEtag = http.patch()
                .uri("/api/properties/{id}/block", created.id())
                .headers(headers -> {
                    headers.setBearerAuth(adminToken);
                    headers.set(HttpHeaders.IF_MATCH, "\"1\"");
                })
                .exchange().expectStatus().isNoContent()
                .returnResult(String.class).getResponseHeaders().getETag();
        putAmenities(ownerToken, created.id(), blockedEtag, List.of())
                .expectStatus().isEqualTo(409);
        assertThat(joinCount(created.id())).isEqualTo(1);
    }

    @Test
    void simultaneousAmenityReplacementsCannotBothCommit() throws Exception {
        String hostToken = createUserAndLogin(UserRole.HOST);
        CreatedProperty created = createProperty(hostToken);
        Amenity wifi = amenities.saveAndFlush(Amenity.create("Wi-Fi-" + UUID.randomUUID()));
        Amenity parking = amenities.saveAndFlush(Amenity.create("Parking-" + UUID.randomUUID()));
        var barrier = new CyclicBarrier(2);
        var executor = Executors.newFixedThreadPool(2);

        Callable<Boolean> selectWifi = () -> replaceAmenitiesInParallel(created.id(), wifi.getId(), barrier);
        Callable<Boolean> selectParking = () -> replaceAmenitiesInParallel(created.id(), parking.getId(), barrier);

        boolean wifiCommitted;
        boolean parkingCommitted;
        try {
            var first = executor.submit(selectWifi);
            var second = executor.submit(selectParking);
            wifiCommitted = first.get(20, TimeUnit.SECONDS);
            parkingCommitted = second.get(20, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
        }

        assertThat(wifiCommitted).isNotEqualTo(parkingCommitted);
        assertThat(jdbc.queryForList(
                "SELECT amenity_id FROM property_amenities WHERE property_id = ?",
                UUID.class, created.id()
        )).containsExactly(wifiCommitted ? wifi.getId() : parking.getId());

        var detail = http.get()
                .uri("/api/properties/{id}/management", created.id())
                .headers(headers -> headers.setBearerAuth(hostToken))
                .exchange().expectStatus().isOk()
                .expectBody(PropertyResponse.class).returnResult();
        assertThat(detail.getResponseBody()).isNotNull();
        assertThat(detail.getResponseBody().version()).isEqualTo(1L);
        assertThat(detail.getResponseHeaders().getETag()).isEqualTo("\"1\"");
    }

    @Test
    void publicDetailShowsSortedAmenitiesForActiveProperty() {
        String hostToken = createUserAndLogin(UserRole.HOST);
        CreatedProperty created = createProperty(hostToken);
        Amenity wifi = amenities.saveAndFlush(Amenity.create("Wi-Fi-" + UUID.randomUUID()));
        Amenity parking = amenities.saveAndFlush(Amenity.create("Parking-" + UUID.randomUUID()));

        String activeEtag = http.patch()
                .uri("/api/properties/{id}/activate", created.id())
                .headers(headers -> {
                    headers.setBearerAuth(hostToken);
                    headers.set(HttpHeaders.IF_MATCH, created.etag());
                })
                .exchange().expectStatus().isNoContent()
                .returnResult(String.class).getResponseHeaders().getETag();

        putAmenities(hostToken, created.id(), activeEtag,
                List.of(wifi.getId(), parking.getId())).expectStatus().isNoContent();

        var detail = http.get()
                .uri("/api/properties/{id}", created.id())
                .exchange().expectStatus().isOk()
                .expectBody(PropertyResponse.class).returnResult();
        assertThat(detail.getResponseBody()).isNotNull();
        assertThat(detail.getResponseBody().amenities())
                .extracting(AmenityResponse::id)
                .containsExactly(parking.getId(), wifi.getId());
        assertThat(detail.getResponseHeaders().getETag()).isEqualTo("\"2\"");
    }

    @Test
    void imageGalleryPersistsOrderAndPrimaryAndRemovesOrphans() {
        String hostToken = createUserAndLogin(UserRole.HOST);
        CreatedProperty created = createProperty(hostToken);
        List<Map<String, Object>> firstGallery = List.of(
                image("https://example.com/front.jpg", false),
                image("https://example.com/room.jpg", true)
        );

        String firstEtag = putImages(hostToken, created.id(), created.etag(), firstGallery)
                .expectStatus().isNoContent()
                .returnResult(String.class).getResponseHeaders().getETag();
        assertThat(firstEtag).isEqualTo("\"1\"");
        assertThat(jdbc.queryForList(
                "SELECT sort_order FROM property_images WHERE property_id = ? ORDER BY sort_order",
                Integer.class, created.id()
        )).containsExactly(0, 1);

        var management = http.get()
                .uri("/api/properties/{id}/management", created.id())
                .headers(headers -> headers.setBearerAuth(hostToken))
                .exchange().expectStatus().isOk()
                .expectBody(PropertyResponse.class).returnResult();
        assertThat(management.getResponseBody()).isNotNull();
        assertThat(management.getResponseBody().images())
                .extracting(PropertyImageResponse::url)
                .containsExactly("https://example.com/front.jpg", "https://example.com/room.jpg");
        assertThat(management.getResponseBody().images())
                .extracting(PropertyImageResponse::primary)
                .containsExactly(false, true);
        assertThat(management.getResponseBody().images())
                .extracting(PropertyImageResponse::sortOrder)
                .containsExactly(0, 1);
        assertThat(management.getResponseHeaders().getETag()).isEqualTo(firstEtag);

        List<UUID> originalImageIds = jdbc.queryForList(
                "SELECT id FROM property_images WHERE property_id = ? ORDER BY sort_order",
                UUID.class, created.id()
        );

        String repeatedEtag = putImages(hostToken, created.id(), firstEtag, firstGallery)
                .expectStatus().isNoContent()
                .returnResult(String.class).getResponseHeaders().getETag();
        assertThat(repeatedEtag).isEqualTo(firstEtag);
        assertThat(jdbc.queryForList(
                "SELECT id FROM property_images WHERE property_id = ? ORDER BY sort_order",
                UUID.class, created.id()
        )).containsExactlyElementsOf(originalImageIds);

        UUID removedImageId = jdbc.queryForObject(
                "SELECT id FROM property_images WHERE property_id = ? AND sort_order = 0",
                UUID.class, created.id()
        );
        String replacedEtag = putImages(hostToken, created.id(), firstEtag,
                List.of(image("https://example.com/room.jpg", true),
                        image("https://example.com/balcony.jpg", false)))
                .expectStatus().isNoContent()
                .returnResult(String.class).getResponseHeaders().getETag();
        assertThat(replacedEtag).isEqualTo("\"2\"");
        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM property_images WHERE id = ?", Integer.class, removedImageId
        )).isZero();

        String clearedEtag = putImages(hostToken, created.id(), replacedEtag, List.of())
                .expectStatus().isNoContent()
                .returnResult(String.class).getResponseHeaders().getETag();
        assertThat(clearedEtag).isEqualTo("\"3\"");
        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM property_images WHERE property_id = ?", Integer.class, created.id()
        )).isZero();
    }

    @Test
    void simultaneousImageReplacementsCannotBothCommit() throws Exception {
        User host = createUser(UserRole.HOST);
        String hostToken = login(host);
        CreatedProperty created = createProperty(hostToken);
        String frontUrl = "https://example.com/front.jpg";
        String roomUrl = "https://example.com/room.jpg";
        var barrier = new CyclicBarrier(2);
        var executor = Executors.newFixedThreadPool(2);

        Callable<Boolean> selectFront = () ->
                replaceImagesInParallel(created.id(), host.getId(), frontUrl, barrier);
        Callable<Boolean> selectRoom = () ->
                replaceImagesInParallel(created.id(), host.getId(), roomUrl, barrier);

        boolean frontCommitted;
        boolean roomCommitted;
        try {
            var first = executor.submit(selectFront);
            var second = executor.submit(selectRoom);
            frontCommitted = first.get(20, TimeUnit.SECONDS);
            roomCommitted = second.get(20, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
        }

        assertThat(frontCommitted).isNotEqualTo(roomCommitted);
        assertThat(jdbc.queryForList(
                "SELECT url FROM property_images WHERE property_id = ?",
                String.class, created.id()
        )).containsExactly(frontCommitted ? frontUrl : roomUrl);

        var detail = http.get()
                .uri("/api/properties/{id}/management", created.id())
                .headers(headers -> headers.setBearerAuth(hostToken))
                .exchange().expectStatus().isOk()
                .expectBody(PropertyResponse.class).returnResult();
        assertThat(detail.getResponseBody()).isNotNull();
        assertThat(detail.getResponseBody().version()).isEqualTo(1L);
        assertThat(detail.getResponseHeaders().getETag()).isEqualTo("\"1\"");
        assertThat(detail.getResponseBody().images())
                .extracting(PropertyImageResponse::url)
                .containsExactly(frontCommitted ? frontUrl : roomUrl);
    }

    @Test
    void imageChangesRequireOwnerCurrentVersionAndValidGallery() {
        String ownerToken = createUserAndLogin(UserRole.HOST);
        String otherHostToken = createUserAndLogin(UserRole.HOST);
        String adminToken = createUserAndLogin(UserRole.ADMIN);
        CreatedProperty created = createProperty(ownerToken);
        List<Map<String, Object>> gallery = List.of(image("https://example.com/front.jpg", true));

        putImages(otherHostToken, created.id(), created.etag(), gallery)
                .expectStatus().isForbidden();
        putImages(ownerToken, created.id(), null, gallery)
                .expectStatus().isEqualTo(428);
        putImages(ownerToken, created.id(), created.etag(), List.of(image("relative.jpg", true)))
                .expectStatus().isBadRequest();
        putImages(ownerToken, created.id(), created.etag(), List.of(image("https://example.com/a.jpg", false)))
                .expectStatus().isBadRequest();
        putImages(ownerToken, created.id(), created.etag(), List.of(
                image("https://example.com/a.jpg", true), image("https://example.com/b.jpg", true)))
                .expectStatus().isBadRequest();
        http.put()
                .uri("/api/properties/{id}/images", created.id())
                .headers(headers -> {
                    headers.setBearerAuth(ownerToken);
                    headers.set(HttpHeaders.IF_MATCH, created.etag());
                })
                .contentType(MediaType.APPLICATION_JSON)
                .body("[{\"url\":\"https://example.com/a.jpg\",\"primary\":null}]")
                .exchange().expectStatus().isBadRequest();
        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM property_images WHERE property_id = ?", Integer.class, created.id()
        )).isZero();

        String imageEtag = putImages(ownerToken, created.id(), created.etag(), gallery)
                .expectStatus().isNoContent()
                .returnResult(String.class).getResponseHeaders().getETag();
        putImages(ownerToken, created.id(), created.etag(), List.of())
                .expectStatus().isEqualTo(412);

        String activeEtag = http.patch()
                .uri("/api/properties/{id}/activate", created.id())
                .headers(headers -> {
                    headers.setBearerAuth(ownerToken);
                    headers.set(HttpHeaders.IF_MATCH, imageEtag);
                })
                .exchange().expectStatus().isNoContent()
                .returnResult(String.class).getResponseHeaders().getETag();
        var publicDetail = http.get()
                .uri("/api/properties/{id}", created.id())
                .exchange().expectStatus().isOk()
                .expectBody(PropertyResponse.class).returnResult();
        assertThat(publicDetail.getResponseBody()).isNotNull();
        assertThat(publicDetail.getResponseBody().images())
                .extracting(PropertyImageResponse::url)
                .containsExactly("https://example.com/front.jpg");

        String blockedEtag = http.patch()
                .uri("/api/properties/{id}/block", created.id())
                .headers(headers -> {
                    headers.setBearerAuth(adminToken);
                    headers.set(HttpHeaders.IF_MATCH, activeEtag);
                })
                .exchange().expectStatus().isNoContent()
                .returnResult(String.class).getResponseHeaders().getETag();
        putImages(ownerToken, created.id(), blockedEtag, List.of())
                .expectStatus().isEqualTo(409);
        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM property_images WHERE property_id = ?", Integer.class, created.id()
        )).isEqualTo(1);
    }

    private static Map<String, Object> image(String url, boolean primary) {
        return Map.of("url", url, "primary", primary);
    }

    private boolean replaceImagesInParallel(UUID propertyId, UUID hostId, String url, CyclicBarrier barrier) {
        Jwt jwt = Jwt.withTokenValue("concurrent-image-test")
                .header("alg", "none")
                .subject(hostId.toString())
                .claim("role", UserRole.HOST.name())
                .build();
        var securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(new JwtAuthenticationToken(jwt));
        SecurityContextHolder.setContext(securityContext);

        try {
            transactions.executeWithoutResult(status -> {
                Property property = properties.findById(propertyId).orElseThrow();
                assertThat(property.getVersion()).isZero();
                assertThat(property.getImages()).isEmpty();

                try {
                    barrier.await(10, TimeUnit.SECONDS);
                } catch (Exception ex) {
                    throw new AssertionError("Both transactions must read the same gallery and version", ex);
                }

                long version = propertyService.replaceImages(propertyId,
                        List.of(new PropertyImageDetails(url, true)), 0);
                assertThat(version).isEqualTo(1L);
            });
            return true;
        } catch (EntityVersionConflictException | ObjectOptimisticLockingFailureException | OptimisticLockException ex) {
            return false;
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private RestTestClient.ResponseSpec putImages(
            String hostToken, UUID propertyId, String etag, List<Map<String, Object>> images
    ) {
        return http.put()
                .uri("/api/properties/{id}/images", propertyId)
                .headers(headers -> {
                    headers.setBearerAuth(hostToken);
                    if (etag != null) {
                        headers.set(HttpHeaders.IF_MATCH, etag);
                    }
                })
                .contentType(MediaType.APPLICATION_JSON)
                .body(images)
                .exchange();
    }

    private int joinCount(UUID propertyId) {
        Integer count = jdbc.queryForObject(
                "SELECT count(*) FROM property_amenities WHERE property_id = ?",
                Integer.class, propertyId);
        assertThat(count).isNotNull();
        return count;
    }

    private boolean replaceAmenitiesInParallel(UUID propertyId, UUID amenityId, CyclicBarrier barrier) {
        try {
            transactions.executeWithoutResult(status -> {
                Property property = properties.findById(propertyId).orElseThrow();
                Amenity amenity = amenities.findById(amenityId).orElseThrow();
                assertThat(property.getVersion()).isZero();
                property.replaceAmenities(List.of(amenity));

                try {
                    barrier.await(10, TimeUnit.SECONDS);
                } catch (Exception ex) {
                    throw new AssertionError("Both transactions must read the same version before flushing", ex);
                }

                properties.flush();
            });
            return true;
        } catch (ObjectOptimisticLockingFailureException ex) {
            return false;
        }
    }

    private RestTestClient.ResponseSpec putAmenities(
            String hostToken, UUID propertyId, String etag, List<UUID> amenityIds
    ) {
        return http.put()
                .uri("/api/properties/{id}/amenities", propertyId)
                .headers(headers -> {
                    headers.setBearerAuth(hostToken);
                    if (etag != null) {
                        headers.set(HttpHeaders.IF_MATCH, etag);
                    }
                })
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("amenityIds", amenityIds))
                .exchange();
    }

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
