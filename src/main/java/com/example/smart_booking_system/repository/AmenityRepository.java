package com.example.smart_booking_system.repository;
import com.example.smart_booking_system.entity.Amenity;
import com.example.smart_booking_system.enums.AmenityType;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AmenityRepository extends JpaRepository<Amenity, Integer> {

    @Query("""
            select a from Amenity a
            where a.isActive = true
            """)
    public List<Amenity> findAllActive();

    @Query("""
            SELECT COUNT(a) > 0 FROM Amenity a 
            WHERE LOWER(a.amenityName) = LOWER(:name)
        """)
    boolean existsBynameIgnoreCase(@Param("name") String name);
    @Query("""
        SELECT a FROM Amenity a
        WHERE a.isActive = true 
        AND a.amenityType = :type
       """)
    List<Amenity> findByType(@Param("type") AmenityType type);


}
