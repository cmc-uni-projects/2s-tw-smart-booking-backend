package com.example.smart_booking_system.repository;

import com.example.smart_booking_system.entity.PropertyAmenity;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PropertyAmenityRepository extends JpaRepository<PropertyAmenity, Integer> {

    @Query("""
            SELECT COUNT(pa) > 0 FROM PropertyAmenity pa
            WHERE pa.propertyId.propertyId = :propertyId
            AND pa.amenityId.amenityId = :amenityId
            AND pa.active = true
            """)
    boolean existsActive(@Param("propertyId") int propertyId,
                         @Param("amenityId") int amenityId);

    @Query("""
            SELECT pa FROM PropertyAmenity pa
            WHERE pa.propertyId.propertyId = :propertyId
            AND pa.active = true
            """)
    List<PropertyAmenity> findActiveByPropertyId(@Param("propertyId") int propertyId);

    @Query("""
            SELECT COUNT(pa) > 0 FROM PropertyAmenity pa
            WHERE pa.propertyId.propertyId = :propertyId
            AND pa.amenityId.amenityId = :amenityId
            AND pa.active = true
            AND pa.propertyAmenityId <> :currentId
            """)
    boolean existsActiveExcept(@Param("propertyId") int propertyId,
                               @Param("amenityId") int amenityId,
                               @Param("currentId") int currentId);
}
