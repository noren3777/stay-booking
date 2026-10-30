package dev.noren.staybooking.property.internal.application.input;

import dev.noren.staybooking.bootstrap.exception.InvalidDomainArgumentException;
import dev.noren.staybooking.bootstrap.pagination.PaginationCriteria;
import dev.noren.staybooking.bootstrap.pagination.SortCriteria;
import dev.noren.staybooking.bootstrap.pagination.SortDirection;
import dev.noren.staybooking.property.internal.domain.enums.PropertyStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public record PropertyCatalogCriteria(PropertyFilterCriteria filters, PaginationCriteria pagination) {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("createdAt", "pricePerNight", "title");
    private static final SortCriteria DEFAULT_SORT = new SortCriteria("createdAt", SortDirection.DESC);
    private static final SortCriteria TIE_BREAKER = new SortCriteria("id", SortDirection.ASC);

    public PropertyCatalogCriteria {
        Objects.requireNonNull(filters, "filters must not be null");
        Objects.requireNonNull(pagination, "pagination must not be null");
        for (SortCriteria item : pagination.sort()) {
            if (!ALLOWED_SORT_FIELDS.contains(item.field())) {
                throw new InvalidDomainArgumentException("Unsupported sort field: " + item.field());
            }
        }
    }

    public PropertyStatus resolvedStatus() {
        return Objects.requireNonNullElse(filters.status(), PropertyStatus.ACTIVE);
    }

    public PaginationCriteria resolvedPagination() {
        List<SortCriteria> sort = new ArrayList<>();
        if (pagination.sort().isEmpty()) {
            sort.add(DEFAULT_SORT);
        } else {
            sort.addAll(pagination.sort());
        }
        sort.add(TIE_BREAKER);
        return new PaginationCriteria(pagination.page(), pagination.size(), sort);
    }
}
