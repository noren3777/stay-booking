package dev.noren.staybooking.bootstrap.exception;

public class InvalidPaginationException extends IllegalArgumentException {
    public InvalidPaginationException(String message) {
        super(message);
    }
}
