package dev.noren.staybooking.property;

import dev.noren.staybooking.bootstrap.domain.FieldChange;
import dev.noren.staybooking.bootstrap.domain.Money;
import dev.noren.staybooking.property.internal.domain.entity.Property;
import dev.noren.staybooking.property.internal.domain.input.CreatePropertyDetails;
import dev.noren.staybooking.property.internal.domain.input.PropertyDetailsPatch;
import jakarta.persistence.EntityManager;

import java.util.UUID;

public final class PropertyBookingTestFixtures {

    private PropertyBookingTestFixtures() {
    }

    public static UUID persistProperty(EntityManager entityManager, UUID hostId, Money price, boolean active) {
        Property property = Property.create(hostId, new CreatePropertyDetails(
                "Flat near the park", null, "Ukraine", "Kyiv", "14 Khreshchatyk Street", price, 3
        ));
        if (active) {
            property.activate();
        }
        entityManager.persist(property);
        entityManager.flush();
        return property.getId();
    }

    public static void updatePrice(EntityManager entityManager, UUID propertyId, Money price) {
        Property property = entityManager.find(Property.class, propertyId);
        property.updateDetails(new PropertyDetailsPatch(
                null, null, null, null, null, new FieldChange<>(true, price), null
        ));
    }

    public static void delete(EntityManager entityManager, UUID propertyId) {
        entityManager.find(Property.class, propertyId).delete();
    }

    public static void block(EntityManager entityManager, UUID propertyId) {
        entityManager.find(Property.class, propertyId).block();
    }
}
