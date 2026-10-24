package dev.noren.staybooking.property.internal.domain.entity;

import dev.noren.staybooking.bootstrap.entity.SoftDeletedEntity;
import dev.noren.staybooking.bootstrap.exception.InvalidDomainArgumentException;
import dev.noren.staybooking.bootstrap.exception.InvalidDomainTransitionException;
import dev.noren.staybooking.property.internal.domain.enums.PropertyStatus;
import dev.noren.staybooking.property.internal.domain.input.CreatePropertyDetails;
import dev.noren.staybooking.property.internal.domain.input.PropertyDetailsPatch;
import dev.noren.staybooking.property.internal.domain.input.PropertyImageDetails;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "properties")
public class Property extends SoftDeletedEntity {

    private static final int PRICE_SCALE = 2;
    private static final BigDecimal MAX_PRICE = new BigDecimal("9999999999.99");

    @NotNull
    @Column(nullable = false, name = "host_id")
    private UUID hostId;

    @NotBlank
    @Size(max = 255)
    @Column(nullable = false)
    private String title;

    @Size(max = 5000)
    @Column(length = 5000)
    private String description;

    @NotBlank
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String country;

    @NotBlank
    @Column(nullable = false, length = 100)
    @Size(max = 100)
    private String city;

    @NotBlank
    @Size(max = 255)
    @Column(nullable = false)
    private String address;

    @NotNull
    @Column(nullable = false, name = "price_per_night", precision = 12, scale = 2)
    @Digits(integer = 10, fraction = 2)
    @Positive
    private BigDecimal pricePerNight;

    @NotNull
    @Column(nullable = false, name = "max_guests")
    @Positive
    private Integer maxGuests;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PropertyStatus status;

    @Version
    @Column(nullable = false)
    private Long version;

    @Getter(AccessLevel.NONE)
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "property_amenities",
            joinColumns = @JoinColumn(name = "property_id"),
            inverseJoinColumns = @JoinColumn(name = "amenity_id")
    )
    private Set<Amenity> amenities = new HashSet<>();

    @Getter(AccessLevel.NONE)
    @OneToMany(mappedBy = "property", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<PropertyImage> images = new ArrayList<>();

    public static Property create(UUID hostId, CreatePropertyDetails details) {
        if (details == null) {
            throw new InvalidDomainArgumentException("details cannot be null");
        }

        Property property = new Property();

        property.status = PropertyStatus.INACTIVE;
        property.title = property.normalizeTitle(details.title());
        property.description = property.normalizeDescription(details.description());
        property.country = property.normalizeCountry(details.country());
        property.city = property.normalizeCity(details.city());
        property.address = property.normalizeAddress(details.address());
        property.maxGuests = property.validateCapacity(details.maxGuests());
        property.pricePerNight = property.normalizePrice(details.pricePerNight());
        property.hostId = property.validatedHostId(hostId);

        return property;
    }

    public void updateDetails(PropertyDetailsPatch details) {
        if (details == null) {
            throw new InvalidDomainArgumentException("details cannot be null");
        }

        requireNotDeleted();
        requireNotBlocked();

        String newTitle = details.title() != null && details.title().specified()
                ? normalizeTitle(details.title().value())
                : title;
        String newDescription = details.description() != null && details.description().specified()
                ? normalizeDescription(details.description().value())
                : description;
        String newCountry = details.country() != null && details.country().specified()
                ? normalizeCountry(details.country().value())
                : country;
        String newCity = details.city() != null && details.city().specified()
                ? normalizeCity(details.city().value())
                : city;
        String newAddress = details.address() != null && details.address().specified()
                ? normalizeAddress(details.address().value())
                : address;
        Integer newMaxGuests = details.maxGuests() != null && details.maxGuests().specified()
                ? validateCapacity(details.maxGuests().value())
                : maxGuests;
        BigDecimal newPricePerNight = details.pricePerNight() != null && details.pricePerNight().specified()
                ? normalizePrice(details.pricePerNight().value())
                : pricePerNight;

        description = newDescription;
        title = newTitle;
        country = newCountry;
        city = newCity;
        address = newAddress;
        maxGuests = newMaxGuests;
        pricePerNight = newPricePerNight;
    }

    public void replaceAmenities(Collection<Amenity> newAmenities) {
        requireNotDeleted();
        requireNotBlocked();

        if (newAmenities == null) {
            throw new InvalidDomainArgumentException("amenities must not be null");
        }

        Set<UUID> ids = new HashSet<>();
        for (Amenity amenity : newAmenities) {
            if (amenity == null || amenity.getId() == null || !ids.add(amenity.getId())) {
                throw new InvalidDomainArgumentException("amenities must contain distinct persisted values");
            }
        }

        Set<UUID> currentIds = new HashSet<>();
        for (Amenity amenity : amenities) {
            currentIds.add(amenity.getId());
        }
        if (currentIds.equals(ids)) {
            return;
        }

        amenities.clear();
        amenities.addAll(newAmenities);
    }

    public boolean replaceImages(List<PropertyImageDetails> newImages) {
        requireNotDeleted();
        requireNotBlocked();

        List<PropertyImage> replacements = createImages(newImages);
        if (hasSameImages(replacements)) {
            return false;
        }

        images.clear();
        images.addAll(replacements);
        return true;
    }

    private List<PropertyImage> createImages(List<PropertyImageDetails> newImages) {
        if (newImages == null) {
            throw new InvalidDomainArgumentException("images must not be null");
        }

        List<PropertyImage> replacements = new ArrayList<>(newImages.size());
        int primaryCount = 0;
        for (int index = 0; index < newImages.size(); index++) {
            PropertyImageDetails details = newImages.get(index);
            if (details == null) {
                throw new InvalidDomainArgumentException("images must not contain null values");
            }
            PropertyImage image = PropertyImage.create(this, details.url(), index, details.primary());
            replacements.add(image);
            if (image.isPrimary()) {
                primaryCount++;
            }
        }
        if (!replacements.isEmpty() && primaryCount != 1) {
            throw new InvalidDomainArgumentException("images must contain exactly one primary image");
        }
        return replacements;
    }

    private boolean hasSameImages(List<PropertyImage> replacements) {
        if (images.size() != replacements.size()) {
            return false;
        }
        for (int index = 0; index < images.size(); index++) {
            PropertyImage current = images.get(index);
            PropertyImage replacement = replacements.get(index);
            if (!current.getUrl().equals(replacement.getUrl())
                    || current.isPrimary() != replacement.isPrimary()) {
                return false;
            }
        }
        return true;
    }

    public void changeHost(UUID id) {
        requireNotDeleted();

        UUID newHostId = validatedHostId(id);
        if (newHostId.equals(hostId)) {
            throw new InvalidDomainArgumentException("host id must not be the same");
        }

        hostId = newHostId;
    }

    public void activate() {
        requireNotDeleted();

        if (status != PropertyStatus.INACTIVE) {
            throw new InvalidDomainTransitionException("Only inactive property can be activated");
        }

        status = PropertyStatus.ACTIVE;
    }

    public void deactivate() {
        requireNotDeleted();

        if (status != PropertyStatus.ACTIVE) {
            throw new InvalidDomainTransitionException("Only active property can be deactivated");
        }

        status = PropertyStatus.INACTIVE;
    }

    public void block() {
        requireNotDeleted();

        if (status.isBlocked()) {
            throw new InvalidDomainTransitionException("Only available property can be blocked");
        }

        status = PropertyStatus.BLOCKED;
    }

    public void unblock() {
        requireNotDeleted();

        if (!status.isBlocked()) {
            throw new InvalidDomainTransitionException("Only blocked property can be unblocked");
        }

        status = PropertyStatus.INACTIVE;
    }

    @Override
    public void delete() {
        requireNotBlocked();
        super.delete();
    }

    private String normalizeTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new InvalidDomainArgumentException("title must not be blank or null");
        }
        title = title.trim();
        if (title.length() > 255) {
            throw new InvalidDomainArgumentException("title must not exceed 255 characters");
        }
        return title;
    }

    private String normalizeDescription(String description) {
        description = description == null ? null : description.trim();
        if (description != null && description.length() > 5000) {
            throw new InvalidDomainArgumentException("description must not exceed 5000 characters");
        }
        return description;
    }

    private String normalizeCountry(String country) {
        if (country == null || country.isBlank()) {
            throw new InvalidDomainArgumentException("country must not be blank or null");
        }
        country = country.trim();
        if (country.length() > 100) {
            throw new InvalidDomainArgumentException("country must not exceed 100 characters");
        }
        return country;
    }

    private String normalizeCity(String city) {
        if (city == null || city.isBlank()) {
            throw new InvalidDomainArgumentException("city must not be blank or null");
        }
        city = city.trim();
        if (city.length() > 100) {
            throw new InvalidDomainArgumentException("city must not exceed 100 characters");
        }
        return city;
    }

    private String normalizeAddress(String address) {
        if (address == null || address.isBlank()) {
            throw new InvalidDomainArgumentException("address must not be blank or null");
        }

        address = address.trim();
        if (address.length() > 255) {
            throw new InvalidDomainArgumentException("address must not exceed 255 characters");
        }

        return address;
    }

    private BigDecimal normalizePrice(BigDecimal price) {
        if (price == null) {
            throw new InvalidDomainArgumentException("price must not be null");
        }
        if (price.signum() <= 0) {
            throw new InvalidDomainArgumentException("price must be positive");
        }
        if (price.scale() > PRICE_SCALE) {
            throw new InvalidDomainArgumentException("price must have at most " + PRICE_SCALE + " decimal places");
        }
        if (price.compareTo(MAX_PRICE) > 0) {
            throw new InvalidDomainArgumentException("price must not exceed " + MAX_PRICE);
        }

        return price.setScale(PRICE_SCALE, RoundingMode.UNNECESSARY);
    }

    private Integer validateCapacity(Integer capacity) {
        if (capacity == null) {
            throw new InvalidDomainArgumentException("capacity must not be null");
        }
        if (capacity.compareTo(0) <= 0) {
            throw new InvalidDomainArgumentException("capacity must be positive");
        }
        return capacity;
    }

    private UUID validatedHostId(UUID id) {
        if (id == null) {
            throw new InvalidDomainArgumentException("host id must not be null");
        }

        return id;
    }

    public Set<Amenity> getAmenities() {
        return Collections.unmodifiableSet(amenities);
    }

    public List<PropertyImage> getImages() {
        return Collections.unmodifiableList(images);
    }

    public boolean hostIsOwner(UUID id) {
        return hostId.equals(id);
    }

    private void requireNotBlocked() {
        if (status.isBlocked()) {
            throw new InvalidDomainTransitionException("Blocked property cannot be updated or deleted");
        }
    }
}
