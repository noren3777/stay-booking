package dev.noren.staybooking.booking.internal.persistence;

import dev.noren.staybooking.booking.internal.domain.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface BookingRepository extends JpaRepository<Booking, UUID> {
}
