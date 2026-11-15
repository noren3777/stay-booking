package dev.noren.staybooking.bootstrap.exception;

public class InvalidDomainTransitionException extends RuntimeException {
    public InvalidDomainTransitionException(String message) {
        super(message);
    }
}
