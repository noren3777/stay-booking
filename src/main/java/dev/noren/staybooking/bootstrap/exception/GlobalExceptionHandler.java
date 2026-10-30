package dev.noren.staybooking.bootstrap.exception;

import dev.noren.staybooking.bootstrap.exception.dto.ValidationError;
import dev.noren.staybooking.bootstrap.web.exception.PreconditionRequiredException;
import jakarta.persistence.OptimisticLockException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        List<ValidationError> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> new ValidationError(
                        error.getField(),
                        error.getDefaultMessage()
                ))
                .toList();

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Request validation failed"
        );
        problem.setProperty("errors", errors);

        log.warn("Validation failed: {}", problem);

        return problem;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ProblemDetail handleHttpMessageNotReadableException() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Request validation failed");
    }

    @ExceptionHandler(EntityNotFoundException.class)
    ProblemDetail handleEntityNotFound(EntityNotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(InvalidDomainArgumentException.class)
    ProblemDetail handleInvalidDomainArgument(InvalidDomainArgumentException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(InvalidPaginationException.class)
    ProblemDetail handleInvalidPagination(InvalidPaginationException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(InvalidDomainTransitionException.class)
    ProblemDetail handleInvalidDomainTransition(InvalidDomainTransitionException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(EntityVersionConflictException.class)
    ProblemDetail handleEntityVersionConflict(EntityVersionConflictException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.PRECONDITION_FAILED, ex.getMessage());
    }

    @ExceptionHandler(PreconditionRequiredException.class)
    ProblemDetail handlePreconditionRequired(PreconditionRequiredException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.PRECONDITION_REQUIRED, ex.getMessage());
    }

    @ExceptionHandler({ObjectOptimisticLockingFailureException.class, OptimisticLockException.class})
    ProblemDetail handleOptimisticLock() {
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.PRECONDITION_FAILED,
                "Entity was modified by another request. Reload it and try again."
        );
    }

}
