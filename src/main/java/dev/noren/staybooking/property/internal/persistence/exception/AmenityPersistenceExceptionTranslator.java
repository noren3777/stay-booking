package dev.noren.staybooking.property.internal.persistence.exception;

import dev.noren.staybooking.property.internal.application.exception.AmenityAlreadyExistsException;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

@Component
public class AmenityPersistenceExceptionTranslator {

    public RuntimeException translate(DataIntegrityViolationException ex) {
        Throwable cause = ex;

        while (cause != null) {
            if (cause instanceof ConstraintViolationException violation
                    && "uc_amenities_name_lower".equals(violation.getConstraintName())) {
                return new AmenityAlreadyExistsException();
            }
            cause = cause.getCause();
        }

        return ex;
    }
}
