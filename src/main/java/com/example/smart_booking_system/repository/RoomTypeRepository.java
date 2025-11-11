package com.example.smart_booking_system.repository;

import com.example.smart_booking_system.entity.RoomType;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface RoomTypeRepository extends JpaRepository<RoomType, Integer> {
    @Query("""
           SELECT r 
           FROM RoomType r 
           WHERE r.propertyId.propertyId = :propertyId
           """)
    List<RoomType> findRoomTypesByPropertyId(@Param("propertyId") int propertyId);

    @Query("""
           SELECT r 
           FROM RoomType r 
           WHERE r.roomTypeId = :roomTypeId
           """)
    Optional<RoomType> findRoomTypeById(@Param("roomTypeId") int roomTypeId);
}
