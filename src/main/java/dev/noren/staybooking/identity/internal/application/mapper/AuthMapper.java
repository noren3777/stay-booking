package dev.noren.staybooking.identity.internal.application.mapper;

import dev.noren.staybooking.identity.internal.application.result.UserResult;
import dev.noren.staybooking.identity.internal.domain.entity.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AuthMapper {

    UserResult toResult(User user);

}
