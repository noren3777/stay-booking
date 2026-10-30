package dev.noren.staybooking.property.internal.application.mapper;

import dev.noren.staybooking.property.internal.application.result.AmenityResult;
import dev.noren.staybooking.property.internal.domain.entity.Amenity;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AmenityApplicationMapper {

    AmenityResult toResult(Amenity amenity);

    List<AmenityResult> toResult(List<Amenity> amenities);

}
