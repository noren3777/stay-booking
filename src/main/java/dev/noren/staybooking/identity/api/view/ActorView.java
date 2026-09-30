package dev.noren.staybooking.identity.api.view;

import java.util.UUID;

public record ActorView(
        UUID id,
        ActorRole actorRole
) {
}
