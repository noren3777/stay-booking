package dev.noren.staybooking.identity.internal.web.handler;

import dev.noren.staybooking.identity.internal.application.exception.EmailAlreadyExistsException;
import dev.noren.staybooking.identity.internal.application.exception.InvalidRegisterParameterException;
import dev.noren.staybooking.identity.internal.application.exception.UserNotFoundException;
import dev.noren.staybooking.bootstrap.web.ApiProblem;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class AuthExceptionHandler {

    @ExceptionHandler(InvalidRegisterParameterException.class)
    ProblemDetail handleInvalidRegistrationParameter(InvalidRegisterParameterException ex, HttpServletRequest request) {
        return ApiProblem.of(HttpStatus.BAD_REQUEST, HttpStatus.BAD_REQUEST.name(), ex.getMessage(), request);
    }

    @ExceptionHandler(UserNotFoundException.class)
    ProblemDetail handleUserNotFoundException(UserNotFoundException ex, HttpServletRequest request) {
        return ApiProblem.of(HttpStatus.NOT_FOUND, HttpStatus.NOT_FOUND.name(), ex.getMessage(), request);
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    ProblemDetail handleEmailAlreadyExistsException(EmailAlreadyExistsException ex, HttpServletRequest request) {
        return ApiProblem.of(HttpStatus.CONFLICT, HttpStatus.CONFLICT.name(), ex.getMessage(), request);
    }

}
