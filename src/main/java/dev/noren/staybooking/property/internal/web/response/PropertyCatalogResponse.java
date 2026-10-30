package dev.noren.staybooking.property.internal.web.response;

import java.util.List;

public record PropertyCatalogResponse(
        List<PropertySummaryResponse> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
