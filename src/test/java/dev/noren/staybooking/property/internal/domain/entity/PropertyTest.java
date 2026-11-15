package dev.noren.staybooking.property.internal.domain.entity;

import dev.noren.staybooking.bootstrap.domain.FieldChange;
import dev.noren.staybooking.bootstrap.domain.Money;
import dev.noren.staybooking.bootstrap.exception.EntityAlreadyDeletedException;
import dev.noren.staybooking.bootstrap.exception.InvalidDomainArgumentException;
import dev.noren.staybooking.bootstrap.exception.InvalidDomainTransitionException;
import dev.noren.staybooking.property.internal.domain.enums.PropertyStatus;
import dev.noren.staybooking.property.internal.domain.input.CreatePropertyDetails;
import dev.noren.staybooking.property.internal.domain.input.PropertyDetailsPatch;
import dev.noren.staybooking.property.internal.domain.input.PropertyImageDetails;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PropertyTest {

    private static final UUID HOST_ID =
            UUID.fromString("11111111-1111-1111-1111-111111111111");

    private static final UUID OTHER_HOST_ID =
            UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Test
    void createsInactivePropertyAndNormalizesDetails() {
        Property property = Property.create(
                HOST_ID,
                new CreatePropertyDetails(
                        "  Flat near the park  ",
                        "  Second floor, balcony  ",
                        "  Ukraine  ",
                        "  Kyiv  ",
                        "  14 Khreshchatyk Street  ",
                        Money.of(new BigDecimal("100")),
                        2
                )
        );

        assertThat(property.getHostId()).isEqualTo(HOST_ID);
        assertThat(property.getTitle()).isEqualTo("Flat near the park");
        assertThat(property.getDescription()).isEqualTo("Second floor, balcony");
        assertThat(property.getCountry()).isEqualTo("Ukraine");
        assertThat(property.getCity()).isEqualTo("Kyiv");
        assertThat(property.getAddress()).isEqualTo("14 Khreshchatyk Street");
        assertThat(property.getPricePerNight())
                .isEqualTo(Money.of(new BigDecimal("100.00")));
        assertThat(property.getMaxGuests()).isEqualTo(2);
        assertThat(property.getStatus()).isEqualTo(PropertyStatus.INACTIVE);
        assertThat(property.getDeletedAt()).isNull();
    }

    @Test
    void rejectsNullHostAndDetails() {
        assertThatThrownBy(() -> Property.create(null, validDetails()))
                .isInstanceOf(InvalidDomainArgumentException.class);

        assertThatThrownBy(() -> {
            //noinspection DataFlowIssue
            Property.create(HOST_ID, null);
        }).isInstanceOf(InvalidDomainArgumentException.class);
    }

    @Test
    void rejectsBlankRequiredFields() {
        assertInvalidDetails(details(
                " ", "Near the metro", "Ukraine", "Kyiv", "14 Khreshchatyk Street",
                new BigDecimal("100.00"), 2
        ));

        assertInvalidDetails(details(
                "Studio apartment", "Near the metro", " ", "Kyiv", "14 Khreshchatyk Street",
                new BigDecimal("100.00"), 2
        ));

        assertInvalidDetails(details(
                "Studio apartment", "Near the metro", "Ukraine", null, "14 Khreshchatyk Street",
                new BigDecimal("100.00"), 2
        ));

        assertInvalidDetails(details(
                "Studio apartment", "Near the metro", "Ukraine", "Kyiv", " ",
                new BigDecimal("100.00"), 2
        ));
    }

    @Test
    void rejectsFieldsExceedingTheirMaximumLength() {
        assertInvalidDetails(details(
                "x".repeat(256), null, "Ukraine", "Kyiv", "14 Khreshchatyk Street",
                new BigDecimal("100.00"), 2
        ));

        assertInvalidDetails(details(
                "Studio apartment", "x".repeat(5001), "Ukraine", "Kyiv", "14 Khreshchatyk Street",
                new BigDecimal("100.00"), 2
        ));

        assertInvalidDetails(details(
                "Studio apartment", null, "x".repeat(101), "Kyiv", "14 Khreshchatyk Street",
                new BigDecimal("100.00"), 2
        ));

        assertInvalidDetails(details(
                "Studio apartment", null, "Ukraine", "x".repeat(101), "14 Khreshchatyk Street",
                new BigDecimal("100.00"), 2
        ));

        assertInvalidDetails(details(
                "Studio apartment", null, "Ukraine", "Kyiv", "x".repeat(256),
                new BigDecimal("100.00"), 2
        ));
    }

    @Test
    void rejectsInvalidPrice() {
        assertInvalidPrice(null);
        assertInvalidPrice(new BigDecimal("0"));
        assertInvalidPrice(new BigDecimal("-1"));
        assertInvalidPrice(new BigDecimal("1.001"));
        assertInvalidPrice(new BigDecimal("10000000000.00"));
    }

    @Test
    void rejectsInvalidCapacity() {
        assertInvalidCapacity(null);
        assertInvalidCapacity(0);
        assertInvalidCapacity(-1);
    }

    @Test
    void updatesAllSpecifiedDetails() {
        Property property = property();

        property.updateDetails(new PropertyDetailsPatch(
                change("  Renovated studio  "),
                change("  Freshly renovated  "),
                change("  Poland  "),
                change("  Warsaw  "),
                change("  8 Velyka Vasylkivska Street  "),
                change(Money.of(new BigDecimal("250"))),
                change(4)
        ));

        assertThat(property.getTitle()).isEqualTo("Renovated studio");
        assertThat(property.getDescription()).isEqualTo("Freshly renovated");
        assertThat(property.getCountry()).isEqualTo("Poland");
        assertThat(property.getCity()).isEqualTo("Warsaw");
        assertThat(property.getAddress()).isEqualTo("8 Velyka Vasylkivska Street");
        assertThat(property.getPricePerNight())
                .isEqualTo(Money.of(new BigDecimal("250.00")));
        assertThat(property.getMaxGuests()).isEqualTo(4);
    }

    @Test
    void leavesUnspecifiedFieldsUntouchedAndAllowsClearingDescription() {
        Property property = property();

        property.updateDetails(new PropertyDetailsPatch(
                new FieldChange<>(false, "Ignored title"),
                change(null),
                null,
                null,
                null,
                null,
                null
        ));

        assertThat(property.getTitle()).isEqualTo("Studio apartment");
        assertThat(property.getDescription()).isNull();
        assertThat(property.getCountry()).isEqualTo("Ukraine");
        assertThat(property.getCity()).isEqualTo("Kyiv");
        assertThat(property.getAddress()).isEqualTo("14 Khreshchatyk Street");
        assertThat(property.getPricePerNight())
                .isEqualTo(Money.of(new BigDecimal("100.00")));
        assertThat(property.getMaxGuests()).isEqualTo(2);
    }

    @Test
    void invalidPatchDoesNotPartiallyChangeProperty() {
        Property property = property();

        PropertyDetailsPatch patch = new PropertyDetailsPatch(
                change("Renovated studio"),
                null,
                null,
                null,
                null,
                change(Money.of(BigDecimal.ZERO)),
                null
        );

        assertThatThrownBy(() -> property.updateDetails(patch))
                .isInstanceOf(InvalidDomainArgumentException.class);

        assertThat(property.getTitle()).isEqualTo("Studio apartment");
        assertThat(property.getPricePerNight())
                .isEqualTo(Money.of(new BigDecimal("100.00")));
        assertThat(property.getStatus()).isEqualTo(PropertyStatus.INACTIVE);
    }

    @Test
    void rejectsNullPatch() {
        Property property = property();

        assertThatThrownBy(() -> property.updateDetails(null))
                .isInstanceOf(InvalidDomainArgumentException.class);
    }

    @Test
    void newlyCreatedPropertyIsNotBookable() {
        Property property = property();

        assertThat(property.isBookable()).isFalse();
    }

    @Test
    void activePropertyIsBookable() {
        Property property = property();
        property.activate();

        assertThat(property.isBookable()).isTrue();
    }

    @Test
    void deactivatedPropertyIsNotBookable() {
        Property property = property();
        property.activate();
        property.deactivate();

        assertThat(property.isBookable()).isFalse();
    }

    @Test
    void blockedPropertyIsNotBookable() {
        Property property = property();
        property.activate();
        property.block();

        assertThat(property.isBookable()).isFalse();
    }

    @Test
    void unblockedPropertyIsNotBookableUntilActivatedAgain() {
        Property property = property();
        property.activate();
        property.block();

        property.unblock();

        assertThat(property.isBookable()).isFalse();

        property.activate();

        assertThat(property.isBookable()).isTrue();
    }

    @Test
    void deletedActivePropertyIsNotBookable() {
        Property property = property();
        property.activate();
        property.delete();

        assertThat(property.getStatus()).isEqualTo(PropertyStatus.ACTIVE);
        assertThat(property.isBookable()).isFalse();
    }

    @Test
    void activatesAndDeactivatesProperty() {
        Property property = property();

        property.activate();
        assertThat(property.getStatus()).isEqualTo(PropertyStatus.ACTIVE);

        property.deactivate();
        assertThat(property.getStatus()).isEqualTo(PropertyStatus.INACTIVE);
    }

    @Test
    void rejectsInvalidActivationAndDeactivation() {
        Property property = property();

        assertThatThrownBy(property::deactivate)
                .isInstanceOf(InvalidDomainTransitionException.class);
        assertThat(property.getStatus()).isEqualTo(PropertyStatus.INACTIVE);

        property.activate();

        assertThatThrownBy(property::activate)
                .isInstanceOf(InvalidDomainTransitionException.class);
        assertThat(property.getStatus()).isEqualTo(PropertyStatus.ACTIVE);
    }

    @Test
    void blocksInactiveAndActiveProperty() {
        Property inactive = property();
        inactive.block();
        assertThat(inactive.getStatus()).isEqualTo(PropertyStatus.BLOCKED);

        Property active = property();
        active.activate();
        active.block();
        assertThat(active.getStatus()).isEqualTo(PropertyStatus.BLOCKED);
    }

    @Test
    void unblocksPropertyIntoInactiveStatus() {
        Property property = property();
        property.activate();
        property.block();

        property.unblock();

        assertThat(property.getStatus()).isEqualTo(PropertyStatus.INACTIVE);
    }

    @Test
    void rejectsRepeatedBlockAndUnblock() {
        Property property = property();

        assertThatThrownBy(property::unblock)
                .isInstanceOf(InvalidDomainTransitionException.class);

        property.block();

        assertThatThrownBy(property::block)
                .isInstanceOf(InvalidDomainTransitionException.class);
        assertThat(property.getStatus()).isEqualTo(PropertyStatus.BLOCKED);
    }

    @Test
    void blockedPropertyCannotBeUpdatedActivatedOrDeleted() {
        Property property = property();
        property.block();

        assertThatThrownBy(() -> property.updateDetails(new PropertyDetailsPatch(
                change("Renovated studio"), null, null, null, null, null, null
        ))).isInstanceOf(InvalidDomainTransitionException.class);

        assertThatThrownBy(property::activate)
                .isInstanceOf(InvalidDomainTransitionException.class);

        assertThatThrownBy(property::delete)
                .isInstanceOf(InvalidDomainTransitionException.class);

        assertThat(property.getTitle()).isEqualTo("Studio apartment");
        assertThat(property.getStatus()).isEqualTo(PropertyStatus.BLOCKED);
        assertThat(property.getDeletedAt()).isNull();
    }

    @Test
    void changesHostButRejectsNullAndCurrentHost() {
        Property property = property();

        assertThatThrownBy(() -> property.changeHost(null))
                .isInstanceOf(InvalidDomainArgumentException.class);

        assertThatThrownBy(() -> property.changeHost(HOST_ID))
                .isInstanceOf(InvalidDomainArgumentException.class);

        property.changeHost(OTHER_HOST_ID);

        assertThat(property.getHostId()).isEqualTo(OTHER_HOST_ID);
        assertThat(property.hostIsOwner(HOST_ID)).isFalse();
        assertThat(property.hostIsOwner(OTHER_HOST_ID)).isTrue();
    }

    @Test
    void changingHostOfBlockedPropertyKeepsItBlocked() {
        Property property = property();
        property.block();

        property.changeHost(OTHER_HOST_ID);

        assertThat(property.getHostId()).isEqualTo(OTHER_HOST_ID);
        assertThat(property.getStatus()).isEqualTo(PropertyStatus.BLOCKED);
    }

    @Test
    void deleteMarksPropertyWithoutChangingStatus() {
        Property property = property();

        property.delete();

        assertThat(property.getDeletedAt()).isNotNull();
        assertThat(property.getStatus()).isEqualTo(PropertyStatus.INACTIVE);
    }

    @Test
    void deletedPropertyCannotBeChangedAgain() {
        Property property = property();
        property.delete();

        assertThatThrownBy(property::activate)
                .isInstanceOf(EntityAlreadyDeletedException.class);

        assertThatThrownBy(() -> property.updateDetails(new PropertyDetailsPatch(
                change("Renovated studio"), null, null, null, null, null, null
        ))).isInstanceOf(EntityAlreadyDeletedException.class);

        assertThatThrownBy(() -> property.changeHost(OTHER_HOST_ID))
                .isInstanceOf(EntityAlreadyDeletedException.class);

        assertThatThrownBy(property::delete)
                .isInstanceOf(EntityAlreadyDeletedException.class);

        assertThat(property.getTitle()).isEqualTo("Studio apartment");
        assertThat(property.getHostId()).isEqualTo(HOST_ID);
        assertThat(property.getStatus()).isEqualTo(PropertyStatus.INACTIVE);
    }

    @Test
    void amenityCollectionCannotBeChangedOutsideAggregate() {
        Property property = property();

        property.replaceAmenities(List.of());
        assertThatThrownBy(() -> property.getAmenities().clear())
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> property.replaceAmenities(null))
                .isInstanceOf(InvalidDomainArgumentException.class);

        property.block();
        assertThatThrownBy(() -> property.replaceAmenities(List.of()))
                .isInstanceOf(InvalidDomainTransitionException.class);

        Property deleted = property();
        deleted.delete();
        assertThatThrownBy(() -> deleted.replaceAmenities(List.of()))
                .isInstanceOf(EntityAlreadyDeletedException.class);
    }

    @Test
    void replacesImageGalleryAtomicallyAndKeepsInputOrder() {
        Property property = property();
        List<PropertyImageDetails> images = List.of(
                new PropertyImageDetails("https://example.com/front.jpg", false),
                new PropertyImageDetails("https://example.com/room.jpg", true)
        );

        assertThat(property.replaceImages(images)).isTrue();
        assertThat(property.getImages()).extracting(PropertyImage::getSortOrder)
                .containsExactly(0, 1);
        assertThat(property.getImages()).extracting(PropertyImage::getUrl)
                .containsExactly("https://example.com/front.jpg", "https://example.com/room.jpg");
        assertThat(property.replaceImages(images)).isFalse();
        assertThatThrownBy(() -> property.getImages().clear())
                .isInstanceOf(UnsupportedOperationException.class);

        assertThatThrownBy(() -> property.replaceImages(List.of(
                new PropertyImageDetails("https://example.com/new.jpg", true),
                new PropertyImageDetails("relative.jpg", false)
        ))).isInstanceOf(InvalidDomainArgumentException.class);
        assertThat(property.getImages()).extracting(PropertyImage::getUrl)
                .containsExactly("https://example.com/front.jpg", "https://example.com/room.jpg");

        assertThat(property.replaceImages(List.of())).isTrue();
        assertThat(property.getImages()).isEmpty();
        assertThat(property.replaceImages(List.of())).isFalse();
    }

    @Test
    void rejectsInvalidImageUrlsAndPrimaryCounts() {
        Property property = property();

        for (String url : List.of("", " ", "relative.jpg", "ftp://example.com/a.jpg",
                "https://", "https://example.com/has space.jpg", "https://example.com/" + "x".repeat(2049))) {
            assertThatThrownBy(() -> property.replaceImages(List.of(new PropertyImageDetails(url, true))))
                    .isInstanceOf(InvalidDomainArgumentException.class);
        }
        assertThatThrownBy(() -> property.replaceImages(List.of(new PropertyImageDetails(null, true))))
                .isInstanceOf(InvalidDomainArgumentException.class);
        assertThatThrownBy(() -> property.replaceImages(List.of(new PropertyImageDetails("https://example.com/a.jpg", null))))
                .isInstanceOf(InvalidDomainArgumentException.class);
        assertThatThrownBy(() -> property.replaceImages(List.of(new PropertyImageDetails("https://example.com/a.jpg", false))))
                .isInstanceOf(InvalidDomainArgumentException.class);
        assertThatThrownBy(() -> property.replaceImages(List.of(
                new PropertyImageDetails("https://example.com/a.jpg", true),
                new PropertyImageDetails("https://example.com/b.jpg", true)
        ))).isInstanceOf(InvalidDomainArgumentException.class);
        assertThatThrownBy(() -> property.replaceImages(null))
                .isInstanceOf(InvalidDomainArgumentException.class);
        assertThat(property.getImages()).isEmpty();
    }

    @Test
    void blockedAndDeletedPropertiesRejectImageChanges() {
        Property blocked = property();
        blocked.block();
        assertThatThrownBy(() -> blocked.replaceImages(List.of()))
                .isInstanceOf(InvalidDomainTransitionException.class);

        Property deleted = property();
        deleted.delete();
        assertThatThrownBy(() -> deleted.replaceImages(List.of()))
                .isInstanceOf(EntityAlreadyDeletedException.class);
    }

    private static Property property() {
        return Property.create(HOST_ID, validDetails());
    }

    private static CreatePropertyDetails validDetails() {
        return details(
                "Studio apartment", "Near the metro", "Ukraine", "Kyiv", "14 Khreshchatyk Street",
                new BigDecimal("100.00"), 2
        );
    }

    private static CreatePropertyDetails details(
            String title,
            String description,
            String country,
            String city,
            String address,
            BigDecimal price,
            Integer capacity
    ) {
        return new CreatePropertyDetails(
                title, description, country, city, address, price == null ? null : Money.of(price), capacity
        );
    }

    private static <T> FieldChange<T> change(T value) {
        return new FieldChange<>(true, value);
    }

    private static void assertInvalidDetails(CreatePropertyDetails details) {
        assertThatThrownBy(() -> Property.create(HOST_ID, details))
                .isInstanceOf(InvalidDomainArgumentException.class);
    }

    private static void assertInvalidPrice(BigDecimal price) {
        assertThatThrownBy(() -> Property.create(HOST_ID, details(
                "Studio apartment", "Near the metro", "Ukraine", "Kyiv", "14 Khreshchatyk Street",
                price, 2
        ))).isInstanceOf(InvalidDomainArgumentException.class);
    }

    private static void assertInvalidCapacity(Integer capacity) {
        assertInvalidDetails(details(
                "Studio apartment", "Near the metro", "Ukraine", "Kyiv", "14 Khreshchatyk Street",
                new BigDecimal("100.00"), capacity
        ));
    }
}
