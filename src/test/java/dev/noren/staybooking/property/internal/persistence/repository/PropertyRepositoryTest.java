package dev.noren.staybooking.property.internal.persistence.repository;

import dev.noren.staybooking.identity.internal.domain.entity.User;
import dev.noren.staybooking.identity.internal.domain.enums.UserRole;
import dev.noren.staybooking.identity.internal.persistence.UserRepository;
import dev.noren.staybooking.property.internal.domain.entity.Property;
import dev.noren.staybooking.property.internal.domain.enums.PropertyStatus;
import dev.noren.staybooking.property.internal.domain.input.CreatePropertyDetails;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.TestConstructor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import dev.noren.staybooking.bootstrap.domain.Money;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
@Testcontainers
@RequiredArgsConstructor
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class PropertyRepositoryTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer("postgres:18.6-bookworm")
                    .withDatabaseName("property_repository_test")
                    .withUsername("test")
                    .withPassword("test");

    private final PropertyRepository properties;
    private final UserRepository users;
    private final EntityManager entityManager;
    private final JdbcTemplate jdbc;
    private final PlatformTransactionManager transactionManager;

    @Test
    void reloadsSavedProperty() {
        UUID hostId = saveHost();
        UUID propertyId = properties.saveAndFlush(newProperty(hostId)).getId();
        entityManager.clear();

        Property loaded = properties.findById(propertyId).orElseThrow();

        assertThat(loaded.getHostId()).isEqualTo(hostId);
        assertThat(loaded.getTitle()).isEqualTo("Flat near the park");
        assertThat(loaded.getDescription()).isEqualTo("Second floor, balcony");
        assertThat(loaded.getPricePerNight()).isEqualTo(Money.of(new BigDecimal("125.50")));
        assertThat(loaded.getMaxGuests()).isEqualTo(3);
        assertThat(loaded.getStatus()).isEqualTo(PropertyStatus.INACTIVE);
        assertThat(loaded.getVersion()).isEqualTo(0L);
        assertThat(loaded.getCreatedAt()).isNotNull();
        assertThat(loaded.getUpdatedAt()).isNotNull();
    }

    @Test
    void softDeletedPropertyStaysInTableButDisappearsFromRepository() {
        UUID propertyId = properties.saveAndFlush(newProperty(saveHost())).getId();

        properties.findById(propertyId).orElseThrow().delete();
        entityManager.flush();
        entityManager.clear();

        assertThat(properties.findById(propertyId)).isEmpty();
        assertThat(properties.findByIdAndStatus(propertyId, PropertyStatus.INACTIVE)).isEmpty();
        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM properties WHERE id = ? AND deleted_at IS NOT NULL",
                Long.class,
                propertyId
        )).isEqualTo(1L);
    }

    @Test
    void priceCheckRejectsZero() {
        UUID propertyId = properties.saveAndFlush(newProperty(saveHost())).getId();

        assertThatThrownBy(() -> jdbc.update(
                "UPDATE properties SET price_per_night = 0 WHERE id = ?", propertyId
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void capacityCheckRejectsZero() {
        UUID propertyId = properties.saveAndFlush(newProperty(saveHost())).getId();

        assertThatThrownBy(() -> jdbc.update(
                "UPDATE properties SET max_guests = 0 WHERE id = ?", propertyId
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void hostForeignKeyRejectsMissingUser() {
        UUID propertyId = properties.saveAndFlush(newProperty(saveHost())).getId();

        assertThatThrownBy(() -> jdbc.update(
                "UPDATE properties SET host_id = ? WHERE id = ?",
                UUID.randomUUID(), propertyId
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void flushIncrementsVersionAfterChange() {
        UUID propertyId = properties.saveAndFlush(newProperty(saveHost())).getId();
        entityManager.clear();

        Property property = properties.findById(propertyId).orElseThrow();
        long previousVersion = property.getVersion();
        property.activate();
        entityManager.flush();
        entityManager.clear();

        Property reloaded = properties.findById(propertyId).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(PropertyStatus.ACTIVE);
        assertThat(reloaded.getVersion()).isEqualTo(previousVersion + 1);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void staleVersionCannotOverwriteCommittedChange() {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);

        UUID propertyId = transaction.execute(status ->
                properties.saveAndFlush(newProperty(saveHost())).getId()
        );
        assertThat(propertyId).isNotNull();

        Property stale = transaction.execute(status ->
                properties.findById(propertyId).orElseThrow()
        );
        assertThat(stale).isNotNull();

        transaction.executeWithoutResult(status ->
                properties.findById(propertyId).orElseThrow().activate()
        );

        assertThatThrownBy(() -> transaction.executeWithoutResult(status ->
                properties.saveAndFlush(stale)
        )).isInstanceOf(ObjectOptimisticLockingFailureException.class);

        Property current = transaction.execute(status ->
                properties.findById(propertyId).orElseThrow()
        );
        assertThat(current).isNotNull();
        assertThat(current.getStatus()).isEqualTo(PropertyStatus.ACTIVE);
        assertThat(current.getVersion()).isEqualTo(stale.getVersion() + 1);
    }

    private UUID saveHost() {
        return users.saveAndFlush(
                User.create("host-" + UUID.randomUUID() + "@example.com", "encoded", UserRole.HOST)
        ).getId();
    }

    private static Property newProperty(UUID hostId) {
        return Property.create(hostId, new CreatePropertyDetails(
                "Flat near the park",
                "Second floor, balcony",
                "Ukraine",
                "Kyiv",
                "14 Khreshchatyk Street",
                Money.of(new BigDecimal("125.50")),
                3
        ));
    }
}
