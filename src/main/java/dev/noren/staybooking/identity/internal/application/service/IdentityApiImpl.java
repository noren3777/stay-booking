package dev.noren.staybooking.identity.internal.application.service;

import dev.noren.staybooking.identity.api.IdentityApi;
import dev.noren.staybooking.identity.internal.application.mapper.IdentityApiMapper;
import dev.noren.staybooking.identity.api.view.ActorRole;
import dev.noren.staybooking.identity.api.view.ActorView;
import dev.noren.staybooking.identity.internal.application.provider.Actor;
import dev.noren.staybooking.identity.internal.application.provider.AuthProvider;
import dev.noren.staybooking.identity.internal.domain.enums.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class IdentityApiImpl implements IdentityApi {

    private final AuthProvider authProvider;
    private final UserService userService;
    private final IdentityApiMapper mapper;

    @Override
    public ActorView currentActor() {
        Actor actor = authProvider.currentActor();

        ActorRole actorRole = mapper.toActorRole(actor.role());

        return new ActorView(actor.id(), actorRole);
    }

    @Override
    public boolean hasRole(UUID id, ActorRole role) {
        UserRole userRole = mapper.toUserRole(role);
        return userService.hasRole(id, userRole);
    }

}
