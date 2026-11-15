package dev.noren.staybooking.bootstrap.web.mapper;

import dev.noren.staybooking.bootstrap.domain.FieldChange;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.stereotype.Component;

@Component
public class JsonNullablePatchMapper {

    public <T> FieldChange<T> toFieldChange(JsonNullable<T> field) {
        if (field == null || !field.isPresent()) {
            return new FieldChange<>(false, null);
        }

        return new FieldChange<>(true, field.orElse(null));
    }

}
