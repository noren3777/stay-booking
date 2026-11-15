package dev.noren.staybooking.property.internal.application.authorization;

import dev.noren.staybooking.identity.api.IdentityApi;
import dev.noren.staybooking.identity.api.view.ActorRole;
import dev.noren.staybooking.identity.api.view.ActorView;
import dev.noren.staybooking.property.internal.domain.entity.Property;
import dev.noren.staybooking.property.internal.domain.enums.PropertyStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PropertyAuthorization {

    private final IdentityApi identityApi;

    public ActorView requireCurrentHost() {
        return requireCurrentActorWithRole(ActorRole.HOST);
    }

    public void requireCurrentAdmin() {
        requireCurrentActorWithRole(ActorRole.ADMIN);
    }

    public void requireCatalogStatusAccess(PropertyStatus status) {
        if (status != PropertyStatus.ACTIVE) {
            requireCurrentAdmin();
        }
    }

    public void requireOwnership(ActorView actor, Property property) {
        if (!property.hostIsOwner(actor.id())) {
            throw new AccessDeniedException("You do not have permission to modify this property");
        }
    }

    private ActorView requireCurrentActorWithRole(ActorRole expectedRole) {
        ActorView actor = identityApi.currentActor();

        if (!actor.hasRole(expectedRole)) {
            throw new AccessDeniedException("You do not have permission to perform this action");
        }

        return actor;
    }

    public void requireManagement(Property property) {
        ActorView actor = identityApi.currentActor();

        if (actor.hasRole(ActorRole.ADMIN)
                || (actor.hasRole(ActorRole.HOST) && property.hostIsOwner(actor.id()))) {
            return;
        }

        throw new AccessDeniedException("You do not have permission to perform this action");
    }

}
