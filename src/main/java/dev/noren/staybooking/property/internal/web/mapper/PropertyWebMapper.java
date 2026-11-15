package dev.noren.staybooking.property.internal.web.mapper;

import dev.noren.staybooking.bootstrap.web.mapper.JsonNullablePatchMapper;
import dev.noren.staybooking.bootstrap.web.mapper.MoneyWebMapper;
import dev.noren.staybooking.bootstrap.web.mapper.PaginationWebMapper;
import dev.noren.staybooking.bootstrap.web.request.PaginationRequest;
import dev.noren.staybooking.property.internal.application.input.PropertyCatalogCriteria;
import dev.noren.staybooking.property.internal.application.result.PropertyResult;
import dev.noren.staybooking.property.internal.application.result.PropertyCatalogResult;
import dev.noren.staybooking.property.internal.domain.input.CreatePropertyDetails;
import dev.noren.staybooking.property.internal.domain.input.PropertyDetailsPatch;
import dev.noren.staybooking.property.internal.domain.input.PropertyImageDetails;
import dev.noren.staybooking.property.internal.web.request.CreatePropertyRequest;
import dev.noren.staybooking.property.internal.web.request.PropertyFilterRequest;
import dev.noren.staybooking.property.internal.web.request.PropertyImageRequest;
import dev.noren.staybooking.property.internal.web.request.UpdatePropertyRequest;
import dev.noren.staybooking.property.internal.web.response.PropertyResponse;
import dev.noren.staybooking.property.internal.web.response.PropertyCatalogResponse;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(
        componentModel = "spring",
        injectionStrategy = InjectionStrategy.CONSTRUCTOR,
        uses = {JsonNullablePatchMapper.class, MoneyWebMapper.class, PaginationWebMapper.class}
)
public interface PropertyWebMapper {
    PropertyResponse toResponse(PropertyResult result);

    PropertyCatalogResponse toResponse(PropertyCatalogResult result);

    PropertyDetailsPatch toDetails(UpdatePropertyRequest request);

    CreatePropertyDetails toDetails(CreatePropertyRequest request);

    @Mapping(target = "filters", source = "filters")
    @Mapping(target = "pagination", source = "pagination")
    PropertyCatalogCriteria toCriteria(PropertyFilterRequest filters, PaginationRequest pagination);

    List<PropertyImageDetails> toImageDetails(List<PropertyImageRequest> images);
}
