package dev.noren.staybooking.property;

import dev.noren.staybooking.identity.internal.domain.entity.User;
import dev.noren.staybooking.identity.internal.domain.enums.UserRole;
import dev.noren.staybooking.identity.internal.persistence.UserRepository;
import dev.noren.staybooking.property.api.PropertyApi;
import dev.noren.staybooking.property.api.view.PropertyBookingView;
import dev.noren.staybooking.property.internal.domain.entity.Property;
import dev.noren.staybooking.property.internal.domain.input.CreatePropertyDetails;
import dev.noren.staybooking.property.internal.persistence.repository.PropertyRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.TestConstructor;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@RequiredArgsConstructor
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@SpringBootTest(properties = {
        "app.jwt.secret-base64=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="
})
class PropertyApiIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer("postgres:18.6-bookworm")
                    .withDatabaseName("property_api_integration_test")
                    .withUsername("test")
                    .withPassword("test");

    private final PropertyApi propertyApi;
    private final PropertyRepository properties;
    private final UserRepository users;
    private final EntityManager entityManager;
    private final TransactionTemplate transactions;

    @Test
    void returnsBookingFactsForActiveProperty() {
        Property property = newProperty();
        property.activate();
        UUID propertyId = saveAndClear(property);

        PropertyBookingView view = propertyApi.findForBooking(propertyId).orElseThrow();

        assertThat(view).isEqualTo(new PropertyBookingView(
                propertyId, property.getHostId(), new BigDecimal("125.50"), 3, true
        ));
    }

    @Test
    void returnsUnbookableViewForInactiveProperty() {
        Property property = newProperty();
        UUID propertyId = saveAndClear(property);

        PropertyBookingView view = propertyApi.findForBooking(propertyId).orElseThrow();

        assertThat(view).isEqualTo(new PropertyBookingView(
                propertyId, property.getHostId(), new BigDecimal("125.50"), 3, false
        ));
    }

    @Test
    void returnsUnbookableViewForBlockedProperty() {
        Property property = newProperty();
        property.activate();
        property.block();
        UUID propertyId = saveAndClear(property);

        PropertyBookingView view = propertyApi.findForBooking(propertyId).orElseThrow();

        assertThat(view).isEqualTo(new PropertyBookingView(
                propertyId, property.getHostId(), new BigDecimal("125.50"), 3, false
        ));
    }

    @Test
    void returnsEmptyForMissingProperty() {
        assertThat(propertyApi.findForBooking(UUID.randomUUID())).isEmpty();
    }

    @Test
    void returnsEmptyForSoftDeletedProperty() {
        Property property = newProperty();
        property.activate();
        UUID propertyId = saveAndClear(property);
        transactions.executeWithoutResult(status -> {
            properties.findById(propertyId).orElseThrow().delete();
            entityManager.flush();
            entityManager.clear();
        });

        assertThat(propertyApi.findForBooking(propertyId)).isEmpty();
    }

    private Property newProperty() {
        UUID hostId = users.saveAndFlush(
                User.create("host-" + UUID.randomUUID() + "@example.com", "encoded", UserRole.HOST)
        ).getId();
        return Property.create(hostId, new CreatePropertyDetails(
                "Flat near the park",
                "Second floor, balcony",
                "Ukraine",
                "Kyiv",
                "14 Khreshchatyk Street",
                new BigDecimal("125.5"),
                3
        ));
    }

    private UUID saveAndClear(Property property) {
        return transactions.execute(status -> {
            UUID propertyId = properties.saveAndFlush(property).getId();
            entityManager.clear();
            return propertyId;
        });
    }
}
