package dev.noren.staybooking.property.internal.application.service;

import dev.noren.staybooking.bootstrap.exception.EntityNotFoundException;
import dev.noren.staybooking.bootstrap.exception.EntityVersionConflictException;
import dev.noren.staybooking.bootstrap.exception.InvalidDomainArgumentException;
import dev.noren.staybooking.bootstrap.persistence.mapper.PaginationPersistenceMapper;
import dev.noren.staybooking.identity.api.IdentityApi;
import dev.noren.staybooking.identity.api.view.ActorRole;
import dev.noren.staybooking.identity.api.view.ActorView;
import dev.noren.staybooking.property.internal.application.authorization.PropertyAuthorization;
import dev.noren.staybooking.property.internal.application.input.PropertyCatalogCriteria;
import dev.noren.staybooking.property.internal.application.mapper.PropertyApplicationMapper;
import dev.noren.staybooking.property.internal.application.result.PropertyCatalogResult;
import dev.noren.staybooking.property.internal.application.result.PropertyResult;
import dev.noren.staybooking.property.internal.application.result.PropertySummaryResult;
import dev.noren.staybooking.property.internal.domain.entity.Amenity;
import dev.noren.staybooking.property.internal.domain.entity.Property;
import dev.noren.staybooking.property.internal.domain.enums.PropertyStatus;
import dev.noren.staybooking.property.internal.domain.input.CreatePropertyDetails;
import dev.noren.staybooking.property.internal.domain.input.PropertyDetailsPatch;
import dev.noren.staybooking.property.internal.domain.input.PropertyImageDetails;
import dev.noren.staybooking.property.internal.persistence.repository.AmenityRepository;
import dev.noren.staybooking.property.internal.persistence.repository.PropertyRepository;
import dev.noren.staybooking.property.internal.persistence.specification.PropertySpecifications;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class PropertyService {

    private final PropertyRepository repository;
    private final AmenityRepository amenityRepository;
    private final PropertyAuthorization authorization;
    private final IdentityApi identityApi;
    private final PropertyApplicationMapper mapper;
    private final PaginationPersistenceMapper paginationMapper;
    private final EntityManager entityManager;

    private Property getById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Property not found"));
    }

    @Transactional(readOnly = true)
    public PropertyCatalogResult list(PropertyCatalogCriteria criteria) {
        PropertyStatus status = criteria.resolvedStatus();
        authorization.requireCatalogStatusAccess(status);

        Pageable pageable = paginationMapper.toPageable(criteria.resolvedPagination());
        Page<PropertySummaryResult> page = repository.findBy(
                PropertySpecifications.forCatalog(criteria.filters(), status),
                query -> query.as(PropertySummaryResult.class).page(pageable)
        );

        return mapper.toCatalogResult(page);
    }

    @Transactional(readOnly = true)
    public PropertyResult getActiveById(UUID id) {
        Property property = repository.findByIdAndStatus(id, PropertyStatus.ACTIVE)
                .orElseThrow(() -> new EntityNotFoundException("Property not found"));
        return toResult(property);
    }

    @Transactional(readOnly = true)
    public PropertyResult getForManagement(UUID id) {
        Property property = getById(id);
        authorization.requireManagement(property);
        return toResult(property);
    }

    public PropertyResult create(CreatePropertyDetails details) {
        ActorView actor = authorization.requireCurrentHost();

        Property property = Property.create(actor.id(), details);

        repository.saveAndFlush(property);
        return toResult(property);
    }

    public PropertyResult update(UUID id, PropertyDetailsPatch details, Long expectedVersion) {
        Property property = getPropertyManagedByCurrentHost(id);
        requireVersion(property, expectedVersion);

        property.updateDetails(details);

        repository.flush();
        return toResult(property);
    }

    public long replaceAmenities(UUID id, List<UUID> amenityIds, long expectedVersion) {
        Property property = getPropertyManagedByCurrentHost(id);
        requireVersion(property, expectedVersion);

        if (amenityIds == null || amenityIds.contains(null)
                || new HashSet<>(amenityIds).size() != amenityIds.size()) {
            throw new InvalidDomainArgumentException("amenityIds must contain distinct non-null values");
        }

        List<Amenity> amenities = amenityRepository.findAllById(amenityIds);
        if (amenities.size() != amenityIds.size()) {
            throw new EntityNotFoundException("Amenity not found");
        }

        property.replaceAmenities(amenities);

        repository.flush();
        return property.getVersion();
    }

    public long replaceImages(UUID id, List<PropertyImageDetails> images, long expectedVersion) {
        Property property = getPropertyManagedByCurrentHost(id);
        requireVersion(property, expectedVersion);

        if (property.replaceImages(images)) {
            entityManager.lock(property, LockModeType.PESSIMISTIC_FORCE_INCREMENT);
        }

        repository.flush();
        return property.getVersion();
    }

    public void delete(UUID id, long expectedVersion) {
        Property property = getPropertyManagedByCurrentHost(id);
        requireVersion(property, expectedVersion);
        property.delete();
    }

    public long activate(UUID id, long expectedVersion) {
        Property property = getPropertyManagedByCurrentHost(id);
        requireVersion(property, expectedVersion);

        property.activate();

        repository.flush();
        return property.getVersion();
    }

    public long deactivate(UUID id, long expectedVersion) {
        Property property = getPropertyManagedByCurrentHost(id);
        requireVersion(property, expectedVersion);

        property.deactivate();

        repository.flush();
        return property.getVersion();
    }

    public long block(UUID id, long expectedVersion) {
        authorization.requireCurrentAdmin();

        Property property = getById(id);
        requireVersion(property, expectedVersion);

        property.block();

        repository.flush();
        return property.getVersion();
    }

    public long unblock(UUID id, long expectedVersion) {
        authorization.requireCurrentAdmin();

        Property property = getById(id);
        requireVersion(property, expectedVersion);

        property.unblock();

        repository.flush();
        return property.getVersion();
    }

    public long changeHost(UUID id, UUID hostId, long expectedVersion) {
        authorization.requireCurrentAdmin();
        if (!identityApi.hasRole(hostId, ActorRole.HOST)) {
            throw new InvalidDomainArgumentException("hostId must refer to a HOST user");
        }

        Property property = getById(id);
        requireVersion(property, expectedVersion);

        property.changeHost(hostId);

        repository.flush();
        return property.getVersion();
    }

    private Property getPropertyManagedByCurrentHost(UUID id) {
        ActorView actor = authorization.requireCurrentHost();
        Property property = getById(id);

        authorization.requireOwnership(actor, property);

        return property;
    }

    private void requireVersion(Property property, long expectedVersion) {
        if (!property.getVersion().equals(expectedVersion)) {
            throw new EntityVersionConflictException();
        }
    }

    private PropertyResult toResult(Property property) {
        List<Amenity> amenities = property.getAmenities().stream()
                .sorted(Comparator.comparing(Amenity::getName).thenComparing(Amenity::getId))
                .toList();
        return mapper.toResult(property, amenities);
    }

}
