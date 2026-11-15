package dev.noren.staybooking.booking.internal.persistence;

import dev.noren.staybooking.booking.internal.domain.entity.Booking;
import dev.noren.staybooking.booking.internal.domain.entity.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

public interface BookingRepository extends JpaRepository<Booking, UUID> {
    @Query("""
            select case when count(booking) > 0 then true else false end
            from Booking booking
            where booking.propertyId = :propertyId
              and booking.status in :statuses
              and booking.checkIn < :checkOut
              and :checkIn < booking.checkOut
            """)
    boolean existsOverlapping(@Param("propertyId") UUID propertyId,
                              @Param("checkIn") LocalDate checkIn,
                              @Param("checkOut") LocalDate checkOut,
                              @Param("statuses") Set<BookingStatus> statuses);
}
