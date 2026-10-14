package dev.noren.staybooking.identity.internal.application.mapper;

import dev.noren.staybooking.identity.api.view.ActorRole;
import dev.noren.staybooking.identity.internal.domain.enums.UserRole;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface IdentityApiMapper {

    UserRole toUserRole(ActorRole role);

    ActorRole toActorRole(UserRole role);

}
