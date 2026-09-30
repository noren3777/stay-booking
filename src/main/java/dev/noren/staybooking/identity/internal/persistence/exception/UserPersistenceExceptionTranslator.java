package dev.noren.staybooking.identity.internal.persistence.exception;

import dev.noren.staybooking.identity.internal.application.exception.EmailAlreadyExistsException;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

@Component
public class UserPersistenceExceptionTranslator {

    public RuntimeException translate(DataIntegrityViolationException ex) {
        Throwable cause = ex;

        while (cause != null) {
            if (cause instanceof ConstraintViolationException violation) {
                return switch (violation.getConstraintName()) {
                    case "uc_users_email" -> new EmailAlreadyExistsException("Email already exists");
                    case null, default -> ex;
                };
            }

            cause = cause.getCause();
        }

        return ex;
    }

}
