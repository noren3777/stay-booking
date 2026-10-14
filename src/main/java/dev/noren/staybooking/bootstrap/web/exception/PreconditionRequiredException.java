package dev.noren.staybooking.bootstrap.web.exception;

public class PreconditionRequiredException extends RuntimeException {
    public PreconditionRequiredException(String message) {
        super(message);
    }
}
