package com.example.smart_booking_system.repository;

import com.example.smart_booking_system.entity.Booking;
import com.example.smart_booking_system.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Integer> {

    // existing overlapping check
    @Query("""
       SELECT b FROM Booking b
       WHERE b.room.roomId = :roomId
         AND b.status IN (
             com.example.smart_booking_system.enums.BookingStatus.CONFIRMED,
             com.example.smart_booking_system.enums.BookingStatus.PENDING_PAYMENT,
             com.example.smart_booking_system.enums.BookingStatus.AWAITING_CONFIRMATION
         )
         AND NOT (b.checkOutDate <= :checkInDate OR b.checkInDate >= :checkOutDate)
       """)
    List<Booking> findConfirmedOverlappingByRoomId(
            @Param("roomId") int roomId,
            @Param("checkInDate") LocalDate checkInDate,
            @Param("checkOutDate") LocalDate checkOutDate
    );

    // --- NEW: convenient finder methods ---
    List<Booking> findByUserUserId(String userId);

    List<Booking> findByPropertyPropertyId(int propertyId);

    List<Booking> findByStatus(BookingStatus status);
}
