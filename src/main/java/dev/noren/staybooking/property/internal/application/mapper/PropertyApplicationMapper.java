package dev.noren.staybooking.property.internal.application.mapper;

import dev.noren.staybooking.property.internal.application.result.PropertyResult;
import dev.noren.staybooking.property.internal.domain.entity.Amenity;
import dev.noren.staybooking.property.internal.domain.entity.Property;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(
        componentModel = "spring",
        injectionStrategy = InjectionStrategy.CONSTRUCTOR,
        uses = AmenityApplicationMapper.class
)
public interface PropertyApplicationMapper {

    @Mapping(target = "amenities", source = "amenities")
    PropertyResult toResult(Property property, List<Amenity> amenities);

}
