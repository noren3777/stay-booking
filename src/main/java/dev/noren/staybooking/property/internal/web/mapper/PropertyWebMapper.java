package dev.noren.staybooking.property.internal.web.mapper;

import dev.noren.staybooking.bootstrap.web.mapper.JsonNullablePatchMapper;
import dev.noren.staybooking.property.internal.domain.entity.Property;
import dev.noren.staybooking.property.internal.domain.input.CreatePropertyDetails;
import dev.noren.staybooking.property.internal.domain.input.PropertyDetailsPatch;
import dev.noren.staybooking.property.internal.web.request.CreatePropertyRequest;
import dev.noren.staybooking.property.internal.web.request.UpdatePropertyRequest;
import dev.noren.staybooking.property.internal.web.response.PropertyResponse;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;

@Mapper(
        componentModel = "spring",
        injectionStrategy = InjectionStrategy.CONSTRUCTOR,
        uses = JsonNullablePatchMapper.class
)
public interface PropertyWebMapper {
    PropertyResponse toResponse(Property property);

    PropertyDetailsPatch toDetails(UpdatePropertyRequest request);

    CreatePropertyDetails toDetails(CreatePropertyRequest request);
}
