package dev.noren.staybooking.identity.internal.application.service;

import dev.noren.staybooking.identity.api.IdentityApi;
import dev.noren.staybooking.identity.api.view.ActorRole;
import dev.noren.staybooking.identity.api.view.ActorView;
import dev.noren.staybooking.identity.internal.application.provider.Actor;
import dev.noren.staybooking.identity.internal.application.provider.AuthProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class IdentityApiImpl implements IdentityApi {

    private final AuthProvider authProvider;

    @Override
    public ActorView currentActor() {
        Actor actor = authProvider.currentActor();

        ActorRole actorRole = switch (actor.role()) {
            case GUEST -> ActorRole.GUEST;
            case HOST -> ActorRole.HOST;
            case ADMIN -> ActorRole.ADMIN;
        };

        return new ActorView(actor.id(), actorRole);
    }

}
