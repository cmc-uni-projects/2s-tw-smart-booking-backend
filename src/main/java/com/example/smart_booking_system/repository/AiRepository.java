package com.example.smart_booking_system.repository;

import com.example.smart_booking_system.entity.Property;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface AiRepository extends JpaRepository<Property, Integer> {

    /**
     * Tìm khách sạn phù hợp theo:
     * 1. Địa điểm (City/Province)
     * 2. Sức chứa (Capacity)
     * 3. Thời gian (CheckIn - CheckOut) -> Loại bỏ phòng đã bị đặt
     */
    @Query("""
        SELECT DISTINCT p FROM Property p
        JOIN Room r ON r.propertyId = p
        WHERE 
        p.isActive = true 
        AND p.propertyStatus = com.example.smart_booking_system.enums.PropertyStatus.APPROVE
        AND r.isActive = true
        AND r.capacity >= :capacity
        AND (:city IS NULL OR :city = '' 
             OR LOWER(p.city) LIKE LOWER(CONCAT('%', :city, '%')) 
             OR LOWER(p.province) LIKE LOWER(CONCAT('%', :city, '%'))
             OR LOWER(p.address) LIKE LOWER(CONCAT('%', :city, '%')))
        AND r.roomId NOT IN (
            SELECT b.room.roomId FROM Booking b
            WHERE b.status <> 'CANCELLED'
            AND (
                (b.checkInDate < :checkOutDate) AND (b.checkOutDate > :checkInDate)
            )
        )
    """)
    List<Property> findAvailableProperties(
            @Param("city") String city,
            @Param("capacity") int capacity,
            @Param("checkInDate") LocalDate checkInDate,
            @Param("checkOutDate") LocalDate checkOutDate
    );
}