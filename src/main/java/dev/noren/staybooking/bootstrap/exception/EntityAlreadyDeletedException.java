package dev.noren.staybooking.bootstrap.exception;

public class EntityAlreadyDeletedException extends InvalidDomainTransitionException {
    public EntityAlreadyDeletedException() {
        super("Entity has already been deleted");
    }
}
