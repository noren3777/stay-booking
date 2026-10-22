package dev.noren.staybooking.property.internal.application.mapper;

import dev.noren.staybooking.property.internal.application.result.PropertyResult;
import dev.noren.staybooking.property.internal.domain.entity.Property;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PropertyApplicationMapper {

    PropertyResult toResult(Property property);

}
