package dev.noren.staybooking.property.internal.persistence.repository;

import dev.noren.staybooking.property.internal.domain.entity.Amenity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AmenityRepository extends JpaRepository<Amenity, UUID> {
    List<Amenity> findAllByOrderByNameAsc();
}
