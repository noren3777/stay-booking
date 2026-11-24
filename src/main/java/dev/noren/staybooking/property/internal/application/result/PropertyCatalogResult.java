package dev.noren.staybooking.property.internal.application.result;

import java.util.List;

public record PropertyCatalogResult(
        List<PropertySummaryResult> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public PropertyCatalogResult {
        items = List.copyOf(items);
    }
}
