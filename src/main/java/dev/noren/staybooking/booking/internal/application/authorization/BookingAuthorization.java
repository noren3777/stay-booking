package dev.noren.staybooking.booking.internal.application.authorization;

import dev.noren.staybooking.identity.api.IdentityApi;
import dev.noren.staybooking.identity.api.view.ActorRole;
import dev.noren.staybooking.identity.api.view.ActorView;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BookingAuthorization {

    private final IdentityApi identityApi;

    public ActorView requireCurrentGuest() {
        ActorView actor = identityApi.currentActor();
        if (!actor.hasRole(ActorRole.GUEST)) {
            throw new AccessDeniedException("You do not have permission to create a booking");
        }
        return actor;
    }
}
