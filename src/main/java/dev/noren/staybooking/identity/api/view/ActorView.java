package dev.noren.staybooking.identity.api.view;

import java.util.UUID;

public record ActorView(
        UUID id,
        ActorRole actorRole
) {

    public boolean hasRole(ActorRole role) {
        return actorRole == role;
    }

}
