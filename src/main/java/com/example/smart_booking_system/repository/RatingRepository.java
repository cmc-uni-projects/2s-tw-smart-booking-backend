package com.example.smart_booking_system.repository;

import com.example.smart_booking_system.entity.Rating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface RatingRepository extends JpaRepository<Rating, Integer> {

    @Query(
            value = "SELECT * FROM Rating WHERE bookings = :bookingId AND isHidden = 0",
            nativeQuery = true
    )
    List<Rating> getRatingByBookingId(int bookingId);

    @Query(
            value = "SELECT * FROM Rating WHERE users = :userId AND isHidden = 0",
            nativeQuery = true
    )
    List<Rating> getRatingByUserId(String userId);

    @Query(
            value = "SELECT * FROM Rating WHERE rating_type = :ratingType AND isHidden = 0",
            nativeQuery = true
    )
    List<Rating> getRatingByRatingType(String ratingType);

    @Query(
            value = "SELECT * FROM Rating WHERE isHidden = 1",
            nativeQuery = true
    )
    List<Rating> getRatingByHidden();
    @Query("""
        SELECT r FROM Rating r
        WHERE r.bookingId.property.propertyId = :propertyId
          AND r.isHidden = false
          AND r.ratingType != com.example.smart_booking_system.enums.RatingType.VIOLATION
    """)
    List<Rating> getRatingsByProperty(int propertyId);

    // Check if a rating exists for a given booking ID
    boolean existsByBookingId_BookingId(int bookingId);

}
