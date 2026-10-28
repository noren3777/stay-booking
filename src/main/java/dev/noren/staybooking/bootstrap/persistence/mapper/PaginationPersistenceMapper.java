package dev.noren.staybooking.bootstrap.persistence.mapper;

import dev.noren.staybooking.bootstrap.pagination.PaginationCriteria;
import dev.noren.staybooking.bootstrap.pagination.SortCriteria;
import dev.noren.staybooking.bootstrap.pagination.SortDirection;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Component
public class PaginationPersistenceMapper {

    public Pageable toPageable(PaginationCriteria pagination) {
        Sort sort = Sort.by(pagination.sort().stream()
                .map(this::toOrder)
                .toList());
        return PageRequest.of(pagination.page(), pagination.size(), sort);
    }

    private Sort.Order toOrder(SortCriteria criterion) {
        Sort.Direction direction = criterion.direction() == SortDirection.ASC
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;
        return new Sort.Order(direction, criterion.field());
    }
}
