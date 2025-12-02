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

    @Query(
            value = """
                SELECT r.* FROM Rating r
                JOIN bookings b ON r.bookings = b.booking_id
                WHERE b.property_id = :propertyId
                  AND r.isHidden = 0
                  AND r.rating_type <> 'VIOLATION'
                """,
            nativeQuery = true
    )
    List<Rating> getRatingsByProperty(int propertyId);

    long countByIsHidden(boolean isHidden);
}