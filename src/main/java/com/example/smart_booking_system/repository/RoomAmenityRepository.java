package com.example.smart_booking_system.repository;

import com.example.smart_booking_system.entity.RoomAmenity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoomAmenityRepository extends JpaRepository<RoomAmenity, Integer> {

    @Query("""
        SELECT ra FROM RoomAmenity ra
        WHERE ra.roomId.roomId = :roomId
        AND ra.active = true
    """)
    List<RoomAmenity> findActiveAmenitiesByRoomId(@Param("roomId") int roomId);

    @Query("""
    SELECT ra FROM RoomAmenity ra
    WHERE ra.roomId.roomId = :roomId
    AND ra.active = true
""")
    List<RoomAmenity> findActiveByRoomId(@Param("roomId") int roomId);

    @Query("""
    SELECT COUNT(ra) > 0 FROM RoomAmenity ra
    WHERE ra.roomId.roomId = :roomId
    AND ra.amenityId.amenityId = :amenityId
    AND ra.active = true
""")
    boolean existsActive(@Param("roomId") int roomId,
                         @Param("amenityId") int amenityId);

    @Query("""
    SELECT COUNT(ra) > 0 FROM RoomAmenity ra
    WHERE ra.roomId.roomId = :roomId
      AND ra.amenityId.amenityId = :amenityId
      AND ra.active = true
      AND ra.roomAmenityId <> :currentId
""")
    boolean existsActiveExcept(
            @Param("roomId") int roomId,
            @Param("amenityId") int amenityId,
            @Param("currentId") int currentId
    );

}
