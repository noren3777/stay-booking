package dev.noren.staybooking.bootstrap.web.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.util.List;

public record PaginationRequest(
        @Min(0) Integer page,
        @Min(1) @Max(100) Integer size,
        List<SortItem> sort
) {
    public PaginationRequest {
        sort = sort == null ? List.of() : List.copyOf(sort);
    }
}
