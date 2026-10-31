package dev.noren.staybooking.property.api;

import dev.noren.staybooking.property.api.view.PropertyBookingView;

import java.util.Optional;
import java.util.UUID;

public interface PropertyApi {

    /**
     * Returns the property facts needed to create a booking.
     * Active properties are bookable; inactive and blocked properties have
     * {@code bookable=false}. The flag does not indicate availability for specific dates.
     *
     * @param propertyId the property identifier
     * @return the booking view, or {@link Optional#empty()} if the property does not exist
     *         or has been soft-deleted
     */
    Optional<PropertyBookingView> findForBooking(UUID propertyId);

}
