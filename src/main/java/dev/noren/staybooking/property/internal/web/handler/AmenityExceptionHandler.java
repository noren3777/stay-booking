package dev.noren.staybooking.property.internal.web.handler;

import dev.noren.staybooking.property.internal.application.exception.AmenityAlreadyExistsException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class AmenityExceptionHandler {

    @ExceptionHandler(AmenityAlreadyExistsException.class)
    ProblemDetail handleAmenityAlreadyExists(AmenityAlreadyExistsException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }
}
