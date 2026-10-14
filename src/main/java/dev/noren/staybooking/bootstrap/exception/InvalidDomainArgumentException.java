package dev.noren.staybooking.bootstrap.exception;

public class InvalidDomainArgumentException extends RuntimeException {
    public InvalidDomainArgumentException(String message) {
        super(message);
    }
}
