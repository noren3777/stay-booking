package dev.noren.staybooking.bootstrap.web.parser;

import dev.noren.staybooking.bootstrap.pagination.SortDirection;
import dev.noren.staybooking.bootstrap.web.request.SortItem;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class SortItemConverter implements Converter<String, SortItem> {

    @Override
    public SortItem convert(String source) {
        String[] parts = source.split(",", -1);
        if (parts.length != 2) {
            throw new IllegalArgumentException("sort must use field,direction format");
        }
        String field = parts[0].trim();
        if (field.isEmpty()) {
            throw new IllegalArgumentException("sort field must not be blank");
        }
        SortDirection direction = switch (parts[1].trim()) {
            case "asc" -> SortDirection.ASC;
            case "desc" -> SortDirection.DESC;
            default -> throw new IllegalArgumentException("sort direction must be asc or desc");
        };
        return new SortItem(field, direction);
    }
}
