package dev.noren.staybooking.bootstrap.exception;

public class EntityVersionConflictException extends RuntimeException {
    public EntityVersionConflictException() {
        super("Entity has been modified by another transaction. Please refresh and try again.");
    }
}
