package com.example.smart_booking_system.repository;

import com.example.smart_booking_system.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Integer> {

    @Query("SELECT r FROM Review r WHERE r.booking.property.propertyId = :propertyId ORDER BY r.createdAt DESC")
    List<Review> findAllByPropertyId(@Param("propertyId") int propertyId);

    boolean existsByBooking_BookingId(int bookingId);
}