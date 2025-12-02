package com.example.smart_booking_system.repository;

import com.example.smart_booking_system.entity.Booking;
import com.example.smart_booking_system.enums.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Integer> {

    List<Booking> findByUserUserId(String userId);

    List<Booking> findByPropertyPropertyId(int propertyId);

    List<Booking> findByStatusAndCreatedAtBefore(BookingStatus status, LocalDateTime dateTime);

    long countByCreatedAtAfter(LocalDateTime date);

    @Query("""
       SELECT b FROM Booking b
       WHERE b.room.roomId = :roomId
         AND b.status IN (
             com.example.smart_booking_system.enums.BookingStatus.CONFIRMED,
             com.example.smart_booking_system.enums.BookingStatus.PENDING_PAYMENT
         )
         AND NOT (b.checkOutDate <= :checkInDate OR b.checkInDate >= :checkOutDate)
       """)
    List<Booking> findConfirmedOverlappingByRoomId(
            @Param("roomId") int roomId,
            @Param("checkInDate") LocalDate checkInDate,
            @Param("checkOutDate") LocalDate checkOutDate
    );

    @Query("""
       SELECT b FROM Booking b
       WHERE b.room.roomId = :roomId
         AND b.checkOutDate >= :today
         AND b.status IN (
             com.example.smart_booking_system.enums.BookingStatus.CONFIRMED,
             com.example.smart_booking_system.enums.BookingStatus.PENDING_PAYMENT,
             com.example.smart_booking_system.enums.BookingStatus.CHECKED_IN
         )
       """)
    List<Booking> findFutureBookingsByRoomId(@Param("roomId") int roomId, @Param("today") LocalDate today);

    @Query("""
        SELECT SUM(b.totalPrice) FROM Booking b
        WHERE b.user.userId = :userId
        AND b.status IN (
            com.example.smart_booking_system.enums.BookingStatus.CONFIRMED,
            com.example.smart_booking_system.enums.BookingStatus.CHECKED_IN,
            com.example.smart_booking_system.enums.BookingStatus.COMPLETED
        )
    """)
    BigDecimal calculateTotalSpentByUser(@Param("userId") String userId);

    // ==========================================
    // 🔥 ADMIN DASHBOARD QUERIES
    // ==========================================

    @Query("SELECT SUM(b.totalPrice) FROM Booking b WHERE b.status IN (com.example.smart_booking_system.enums.BookingStatus.CONFIRMED, com.example.smart_booking_system.enums.BookingStatus.CHECKED_IN, com.example.smart_booking_system.enums.BookingStatus.COMPLETED)")
    BigDecimal calculateGlobalRevenue();

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.status IN (com.example.smart_booking_system.enums.BookingStatus.CONFIRMED, com.example.smart_booking_system.enums.BookingStatus.CHECKED_IN, com.example.smart_booking_system.enums.BookingStatus.COMPLETED)")
    long countTotalConfirmedBookings();

    @Query("SELECT b FROM Booking b WHERE YEAR(b.createdAt) = :year AND b.status IN (com.example.smart_booking_system.enums.BookingStatus.CONFIRMED, com.example.smart_booking_system.enums.BookingStatus.CHECKED_IN, com.example.smart_booking_system.enums.BookingStatus.COMPLETED)")
    List<Booking> findGlobalBookingsByYear(@Param("year") int year);

    @Query("SELECT b.property, COUNT(b), SUM(b.totalPrice) FROM Booking b WHERE b.status IN (com.example.smart_booking_system.enums.BookingStatus.CONFIRMED, com.example.smart_booking_system.enums.BookingStatus.CHECKED_IN, com.example.smart_booking_system.enums.BookingStatus.COMPLETED) GROUP BY b.property ORDER BY COUNT(b) DESC")
    List<Object[]> findTopPropertiesGlobal(Pageable pageable);

    // 🔥 Query mới để lấy danh sách booking mới nhất cho Admin
    Page<Booking> findAllByOrderByCreatedAtDesc(Pageable pageable);

    // ==========================================
    // 🔥 OWNER DASHBOARD QUERIES
    // ==========================================

    @Query("SELECT SUM(b.totalPrice) FROM Booking b WHERE b.property.owner.userId = :ownerId AND b.status IN (com.example.smart_booking_system.enums.BookingStatus.CONFIRMED, com.example.smart_booking_system.enums.BookingStatus.CHECKED_IN, com.example.smart_booking_system.enums.BookingStatus.COMPLETED)")
    BigDecimal calculateOwnerRevenue(@Param("ownerId") String ownerId);

    long countByProperty_Owner_UserId(String ownerId);

    long countByProperty_Owner_UserIdAndCreatedAtAfter(String ownerId, LocalDateTime date);

    @Query("SELECT b FROM Booking b WHERE b.property.owner.userId = :ownerId AND YEAR(b.createdAt) = :year AND b.status IN (com.example.smart_booking_system.enums.BookingStatus.CONFIRMED, com.example.smart_booking_system.enums.BookingStatus.CHECKED_IN, com.example.smart_booking_system.enums.BookingStatus.COMPLETED)")
    List<Booking> findOwnerBookingsByYear(@Param("ownerId") String ownerId, @Param("year") int year);

    @Query("SELECT b.property, COUNT(b), SUM(b.totalPrice) " +
            "FROM Booking b " +
            "WHERE b.property.owner.userId = :ownerId " +
            "AND b.status IN (com.example.smart_booking_system.enums.BookingStatus.CONFIRMED, com.example.smart_booking_system.enums.BookingStatus.CHECKED_IN, com.example.smart_booking_system.enums.BookingStatus.COMPLETED) " +
            "GROUP BY b.property " +
            "ORDER BY SUM(b.totalPrice) DESC")
    List<Object[]> findTopPropertiesByRevenue(@Param("ownerId") String ownerId, Pageable pageable);

    List<Booking> findByProperty_Owner_UserIdOrderByCreatedAtDesc(String ownerId, Pageable pageable);
}