package com.example.smart_booking_system.repository;

import com.example.smart_booking_system.entity.PropertyImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PropertyImageRepository extends JpaRepository<PropertyImage, Integer> {

    @Query("""
        SELECT pi FROM PropertyImage pi
        WHERE pi.property.propertyId = :propertyId
        AND pi.isActive = true
    """)
    List<PropertyImage> findActiveImagesByPropertyId(int propertyId);
}
