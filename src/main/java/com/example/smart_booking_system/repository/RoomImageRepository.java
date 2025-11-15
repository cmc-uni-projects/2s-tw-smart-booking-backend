package com.example.smart_booking_system.repository;

import com.example.smart_booking_system.entity.RoomImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface RoomImageRepository extends JpaRepository<RoomImage, Integer> {

    @Query("""
        SELECT ri FROM RoomImage ri
        WHERE ri.room.roomId = :roomId
        AND ri.isActive = true
    """)
    List<RoomImage> findActiveImagesByRoomId(int roomId);
}
