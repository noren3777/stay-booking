package dev.noren.staybooking.bootstrap.pagination;

import java.util.Objects;

public record SortCriteria(String field, SortDirection direction) {
    public SortCriteria {
        if (field == null || field.isBlank()) {
            throw new IllegalArgumentException("sort field must not be blank");
        }
        Objects.requireNonNull(direction, "sort direction must not be null");
    }
}
