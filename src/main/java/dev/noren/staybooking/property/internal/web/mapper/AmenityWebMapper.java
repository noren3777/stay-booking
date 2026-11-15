package dev.noren.staybooking.property.internal.web.mapper;

import dev.noren.staybooking.property.internal.application.result.AmenityResult;
import dev.noren.staybooking.property.internal.web.response.AmenityResponse;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AmenityWebMapper {

    AmenityResponse toResponse(AmenityResult result);

    List<AmenityResponse> toResponse(List<AmenityResult> results);

}
