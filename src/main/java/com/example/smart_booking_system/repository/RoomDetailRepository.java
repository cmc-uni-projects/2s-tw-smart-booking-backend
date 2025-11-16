package com.example.smart_booking_system.repository;

import com.example.smart_booking_system.entity.Room;
import com.example.smart_booking_system.entity.RoomAmenity;
import com.example.smart_booking_system.entity.RoomImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RoomDetailRepository extends JpaRepository<Room, Integer> {

    @Query("""
           SELECT r
           FROM Room r
           JOIN FETCH r.propertyId
           WHERE r.roomId = :roomId
           """)
    Room getRoomDetail(@Param("roomId") int roomId);


    @Query("""
           SELECT ra
           FROM RoomAmenity ra
           JOIN FETCH ra.amenityId a
           WHERE ra.roomId.roomId = :roomId
           """)
    List<RoomAmenity> getAmenitiesByRoomId(@Param("roomId") int roomId);


    @Query("""
           SELECT ri
           FROM RoomImage ri
           WHERE ri.room.roomId = :roomId
           """)
    List<RoomImage> getImagesByRoomId(@Param("roomId") int roomId);
}
