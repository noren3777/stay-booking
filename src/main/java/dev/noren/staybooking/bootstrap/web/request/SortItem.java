package dev.noren.staybooking.bootstrap.web.request;

import dev.noren.staybooking.bootstrap.pagination.SortDirection;

public record SortItem(
        String field,
        SortDirection direction
) {
}
