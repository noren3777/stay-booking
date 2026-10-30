package dev.noren.staybooking.bootstrap.web.mapper;

import dev.noren.staybooking.bootstrap.pagination.PaginationCriteria;
import dev.noren.staybooking.bootstrap.pagination.SortCriteria;
import dev.noren.staybooking.bootstrap.web.request.PaginationRequest;
import dev.noren.staybooking.bootstrap.web.request.SortItem;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class PaginationWebMapper {

    public PaginationCriteria toCriteria(PaginationRequest request) {
        List<SortCriteria> sort = new ArrayList<>();
        for (SortItem item : request.sort()) {
            sort.add(new SortCriteria(item.field(), item.direction()));
        }

        int page = request.page() == null ? 0 : request.page();
        int size = request.size() == null ? 20 : request.size();
        return new PaginationCriteria(page, size, sort);
    }
}
