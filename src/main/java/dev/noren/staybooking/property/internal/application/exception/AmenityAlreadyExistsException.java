package dev.noren.staybooking.property.internal.application.exception;

public class AmenityAlreadyExistsException extends RuntimeException {

    public AmenityAlreadyExistsException() {
        super("Amenity name already exists");
    }
}
