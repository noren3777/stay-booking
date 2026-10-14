package dev.noren.staybooking.property.internal.persistence.repository;

import dev.noren.staybooking.property.internal.domain.entity.Property;
import dev.noren.staybooking.property.internal.domain.enums.PropertyStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PropertyRepository extends JpaRepository<Property, UUID> {

    Optional<Property> findByIdAndStatus(UUID id, PropertyStatus status);

}
