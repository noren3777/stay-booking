package dev.noren.staybooking.property.internal.application.input;

import dev.noren.staybooking.bootstrap.exception.InvalidDomainArgumentException;
import dev.noren.staybooking.bootstrap.pagination.PaginationCriteria;
import dev.noren.staybooking.bootstrap.pagination.SortCriteria;
import dev.noren.staybooking.bootstrap.pagination.SortDirection;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PropertyCatalogCriteriaTest {

    private static final PropertyFilterCriteria FILTERS =
            new PropertyFilterCriteria(null, null, null, null, null);

    @Test
    void defaultSortIncludesStableTieBreaker() {
        var criteria = new PropertyCatalogCriteria(FILTERS, new PaginationCriteria(0, 20, List.of()));

        assertThat(criteria.resolvedPagination().sort()).containsExactly(
                new SortCriteria("createdAt", SortDirection.DESC),
                new SortCriteria("id", SortDirection.ASC)
        );
    }

    @Test
    void requestedOrderIsPreservedBeforeTieBreaker() {
        var criteria = new PropertyCatalogCriteria(FILTERS, new PaginationCriteria(2, 5, List.of(
                new SortCriteria("pricePerNight", SortDirection.ASC),
                new SortCriteria("title", SortDirection.DESC)
        )));

        PaginationCriteria resolved = criteria.resolvedPagination();
        assertThat(resolved.page()).isEqualTo(2);
        assertThat(resolved.size()).isEqualTo(5);
        assertThat(resolved.sort()).containsExactly(
                new SortCriteria("pricePerNight", SortDirection.ASC),
                new SortCriteria("title", SortDirection.DESC),
                new SortCriteria("id", SortDirection.ASC)
        );
    }

    @Test
    void unsupportedFieldIsRejectedDuringConstruction() {
        PaginationCriteria pagination = new PaginationCriteria(0, 20, List.of(
                new SortCriteria("hostId", SortDirection.ASC)
        ));

        assertThatThrownBy(() -> new PropertyCatalogCriteria(FILTERS, pagination))
                .isInstanceOf(InvalidDomainArgumentException.class);
    }
}
