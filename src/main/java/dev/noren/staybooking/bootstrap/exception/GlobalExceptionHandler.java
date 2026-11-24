package dev.noren.staybooking.bootstrap.exception;

import dev.noren.staybooking.bootstrap.exception.dto.ValidationError;
import dev.noren.staybooking.bootstrap.web.ApiProblem;
import dev.noren.staybooking.bootstrap.web.exception.InvalidVersionHeaderException;
import dev.noren.staybooking.bootstrap.web.exception.PreconditionRequiredException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.persistence.OptimisticLockException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.core.annotation.Order;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Slf4j
@Order
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<ValidationError> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> new ValidationError(
                        error.getField(),
                        error.getDefaultMessage()
                ))
                .toList();

        ProblemDetail problem = ApiProblem.of(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED",
                "Request validation failed", request);
        problem.setProperty("errors", errors);
        return problem;
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class})
    ProblemDetail handleMalformedRequest(HttpServletRequest request) {
        return ApiProblem.of(HttpStatus.BAD_REQUEST, HttpStatus.BAD_REQUEST.name(), "Request is invalid", request);
    }

    @ExceptionHandler(EntityNotFoundException.class)
    ProblemDetail handleEntityNotFound(EntityNotFoundException ex, HttpServletRequest request) {
        return ApiProblem.of(HttpStatus.NOT_FOUND, HttpStatus.NOT_FOUND.name(), ex.getMessage(), request);
    }

    @ExceptionHandler(InvalidDomainArgumentException.class)
    ProblemDetail handleInvalidDomainArgument(InvalidDomainArgumentException ex, HttpServletRequest request) {
        return ApiProblem.of(HttpStatus.BAD_REQUEST, HttpStatus.BAD_REQUEST.name(), ex.getMessage(), request);
    }

    @ExceptionHandler(InvalidPaginationException.class)
    ProblemDetail handleInvalidPagination(InvalidPaginationException ex, HttpServletRequest request) {
        return ApiProblem.of(HttpStatus.BAD_REQUEST, HttpStatus.BAD_REQUEST.name(), ex.getMessage(), request);
    }

    @ExceptionHandler(InvalidDomainTransitionException.class)
    ProblemDetail handleInvalidDomainTransition(InvalidDomainTransitionException ex, HttpServletRequest request) {
        return ApiProblem.of(HttpStatus.CONFLICT, HttpStatus.CONFLICT.name(), ex.getMessage(), request);
    }

    @ExceptionHandler(EntityVersionConflictException.class)
    ProblemDetail handleEntityVersionConflict(EntityVersionConflictException ex, HttpServletRequest request) {
        return ApiProblem.of(HttpStatus.PRECONDITION_FAILED, HttpStatus.PRECONDITION_FAILED.name(), ex.getMessage(), request);
    }

    @ExceptionHandler(PreconditionRequiredException.class)
    ProblemDetail handlePreconditionRequired(PreconditionRequiredException ex, HttpServletRequest request) {
        return ApiProblem.of(HttpStatus.PRECONDITION_REQUIRED, HttpStatus.PRECONDITION_REQUIRED.name(), ex.getMessage(), request);
    }

    @ExceptionHandler(InvalidVersionHeaderException.class)
    ProblemDetail handleInvalidVersionHeader(InvalidVersionHeaderException ex, HttpServletRequest request) {
        return ApiProblem.of(HttpStatus.BAD_REQUEST, HttpStatus.BAD_REQUEST.name(), ex.getMessage(), request);
    }

    @ExceptionHandler({ObjectOptimisticLockingFailureException.class, OptimisticLockException.class})
    ProblemDetail handleOptimisticLock(HttpServletRequest request) {
        return ApiProblem.of(HttpStatus.PRECONDITION_FAILED, HttpStatus.PRECONDITION_FAILED.name(),
                "Entity was modified by another request. Reload it and try again.", request);
    }

    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<ProblemDetail> handleResponseStatus(ResponseStatusException ex, HttpServletRequest request) {
        HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
        if (status == null) {
            return unexpected(ex, request);
        }
        return ResponseEntity.status(status).headers(ex.getHeaders())
                .body(ApiProblem.of(status, status.name(), status.getReasonPhrase(), request));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ProblemDetail handleNoResource(HttpServletRequest request) {
        return ApiProblem.of(HttpStatus.NOT_FOUND, HttpStatus.NOT_FOUND.name(), "Resource not found", request);
    }

    @ExceptionHandler(AuthenticationException.class)
    ProblemDetail handleAuthentication(HttpServletRequest request) {
        return ApiProblem.of(HttpStatus.UNAUTHORIZED, HttpStatus.UNAUTHORIZED.name(), "Authentication required", request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    ProblemDetail handleAccessDenied(HttpServletRequest request) {
        return ApiProblem.of(HttpStatus.FORBIDDEN, HttpStatus.FORBIDDEN.name(), "Access denied", request);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ProblemDetail> unexpected(Exception ex, HttpServletRequest request) {
        if (ex instanceof ErrorResponse error) {
            HttpStatus status = HttpStatus.resolve(error.getStatusCode().value());
            if (status != null && status.is4xxClientError()) {
                return ResponseEntity.status(status).headers(error.getHeaders())
                        .body(ApiProblem.of(status, status.name(), status.getReasonPhrase(), request));
            }
        }
        String requestId = ApiProblem.requestId(request);
        log.error("Unexpected request failure, requestId={}", requestId, ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiProblem.of(HttpStatus.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR.name(),
                        "An unexpected error occurred", request));
    }

}
