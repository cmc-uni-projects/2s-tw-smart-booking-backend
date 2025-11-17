package com.example.smart_booking_system.repository;

import com.example.smart_booking_system.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoomRepository extends JpaRepository<Room, Integer> {
    List<Room> findByPropertyId_PropertyIdAndIsActiveTrue(int propertyId);
    @Query("""
        SELECT r FROM Room r
        WHERE 
        (:propertyId IS NULL OR r.propertyId.propertyId = :propertyId)
        AND
        (:keyword IS NULL OR (
            LOWER(r.roomName) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(r.description) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(r.roomCategory) LIKE LOWER(CONCAT('%', :keyword, '%'))
        ))
        AND r.isActive = true
    """)
    List<Room> searchRooms(
            @Param("propertyId") Integer propertyId,
            @Param("keyword") String keyword
    );
    @Query("""
           SELECT r
           FROM Room r
           WHERE r.propertyId.propertyId = :propertyId
             AND r.roomCategory = :category
           """)
    Optional<Room> findByPropertyIdAndCategory(@Param("propertyId") int propertyId,
                                               @Param("category") com.example.smart_booking_system.enums.RoomCategory category);
}