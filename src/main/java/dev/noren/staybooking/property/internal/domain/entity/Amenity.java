package dev.noren.staybooking.property.internal.domain.entity;

import dev.noren.staybooking.bootstrap.entity.BaseEntity;
import dev.noren.staybooking.bootstrap.exception.InvalidDomainArgumentException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "amenities")
public class Amenity extends BaseEntity {

    @Column(nullable = false, length = 100, updatable = false)
    private String name;

    public static Amenity create(String name) {
        Amenity amenity = new Amenity();
        amenity.name = amenity.normalizeName(name);
        return amenity;
    }

    private String normalizeName(String name) {
        if (name == null) {
            throw new InvalidDomainArgumentException("Amenity name must not be null");
        }

        name = name.trim();
        if (name.isBlank()) {
            throw new InvalidDomainArgumentException("Amenity name must not be blank");
        }
        if (name.length() > 100) {
            throw new InvalidDomainArgumentException("Amenity name must not exceed 100 characters");
        }

        return name;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof Amenity amenity)) return false;
        return getId() != null && getId().equals(amenity.getId());
    }

    @Override
    public int hashCode() {
        return Amenity.class.hashCode();
    }
}
