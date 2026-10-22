package dev.noren.staybooking.property.internal.application.service;

import dev.noren.staybooking.bootstrap.exception.EntityNotFoundException;
import dev.noren.staybooking.bootstrap.exception.EntityVersionConflictException;
import dev.noren.staybooking.bootstrap.exception.InvalidDomainArgumentException;
import dev.noren.staybooking.identity.api.IdentityApi;
import dev.noren.staybooking.identity.api.view.ActorRole;
import dev.noren.staybooking.identity.api.view.ActorView;
import dev.noren.staybooking.property.internal.application.authorization.PropertyAuthorization;
import dev.noren.staybooking.property.internal.application.mapper.PropertyApplicationMapper;
import dev.noren.staybooking.property.internal.application.result.PropertyResult;
import dev.noren.staybooking.property.internal.domain.entity.Property;
import dev.noren.staybooking.property.internal.domain.enums.PropertyStatus;
import dev.noren.staybooking.property.internal.domain.input.CreatePropertyDetails;
import dev.noren.staybooking.property.internal.domain.input.PropertyDetailsPatch;
import dev.noren.staybooking.property.internal.persistence.repository.PropertyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class PropertyService {

    private final PropertyRepository repository;
    private final PropertyAuthorization authorization;
    private final IdentityApi identityApi;
    private final PropertyApplicationMapper mapper;

    private Property getById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Property not found"));
    }

    @Transactional(readOnly = true)
    public PropertyResult getActiveById(UUID id) {
        Property property = repository.findByIdAndStatus(id, PropertyStatus.ACTIVE)
                .orElseThrow(() -> new EntityNotFoundException("Property not found"));
        return mapper.toResult(property);
    }

    @Transactional(readOnly = true)
    public PropertyResult getForManagement(UUID id) {
        Property property = getById(id);
        authorization.requireManagement(property);
        return mapper.toResult(property);
    }

    public PropertyResult create(CreatePropertyDetails details) {
        ActorView actor = authorization.requireCurrentHost();

        Property property = Property.create(actor.id(), details);

        repository.saveAndFlush(property);
        return mapper.toResult(property);
    }

    public PropertyResult update(UUID id, PropertyDetailsPatch details, Long expectedVersion) {
        Property property = getPropertyManagedByCurrentHost(id);

        requireVersion(property, expectedVersion);
        property.updateDetails(details);

        repository.flush();
        return mapper.toResult(property);
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

}
