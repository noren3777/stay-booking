package dev.noren.staybooking.identity.api;

import dev.noren.staybooking.identity.api.view.ActorRole;
import dev.noren.staybooking.identity.api.view.ActorView;

import java.util.UUID;

public interface IdentityApi {

    ActorView currentActor();

    boolean hasRole(UUID id, ActorRole role);

}
