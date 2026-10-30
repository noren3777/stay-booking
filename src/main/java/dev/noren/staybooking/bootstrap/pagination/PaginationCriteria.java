package dev.noren.staybooking.bootstrap.pagination;

import dev.noren.staybooking.bootstrap.exception.InvalidPaginationException;

import java.util.HashSet;
import java.util.List;

public record PaginationCriteria(int page, int size, List<SortCriteria> sort) {
    public PaginationCriteria {
        if (page < 0) {
            throw new InvalidPaginationException("page must not be negative");
        }
        if (size < 1 || size > 100) {
            throw new InvalidPaginationException("size must be between 1 and 100");
        }
        if ((long) page * size > Integer.MAX_VALUE) {
            throw new InvalidPaginationException("page offset exceeds the supported limit");
        }
        sort = List.copyOf(sort);
        var fields = new HashSet<String>();
        for (SortCriteria item : sort) {
            if (!fields.add(item.field())) {
                throw new InvalidPaginationException("sort fields must be distinct");
            }
        }
    }
}
