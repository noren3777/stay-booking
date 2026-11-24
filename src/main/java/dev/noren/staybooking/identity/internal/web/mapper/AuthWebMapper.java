package dev.noren.staybooking.identity.internal.web.mapper;

import dev.noren.staybooking.identity.internal.application.result.TokenResult;
import dev.noren.staybooking.identity.internal.application.result.UserResult;
import dev.noren.staybooking.identity.internal.web.response.TokenResponse;
import dev.noren.staybooking.identity.internal.web.response.UserResponse;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface AuthWebMapper {

    UserResponse toResponse(UserResult result);

    TokenResponse toResponse(TokenResult result);
}
