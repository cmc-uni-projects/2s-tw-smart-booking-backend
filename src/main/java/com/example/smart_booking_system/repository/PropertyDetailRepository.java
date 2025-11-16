package com.example.smart_booking_system.repository;

import com.example.smart_booking_system.entity.Property;
import com.example.smart_booking_system.entity.Room;
import com.example.smart_booking_system.entity.PropertyAmenity;
import com.example.smart_booking_system.entity.PropertyImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PropertyDetailRepository extends JpaRepository<Property, Integer> {

    // ---------------------
    // Lấy property theo ID
    // ---------------------
    @Query("""
           SELECT p 
           FROM Property p
           LEFT JOIN FETCH p.OwnerId
           WHERE p.propertyId = :propertyId
           """)
    Property getPropertyDetail(@Param("propertyId") int propertyId);


    // ---------------------
    // Lấy danh sách room thuộc property
    // ---------------------
    @Query("""
           SELECT r
           FROM Room r
           WHERE r.propertyId.propertyId = :propertyId
           """)
    List<Room> getRoomsByPropertyId(@Param("propertyId") int propertyId);


    // ---------------------
    // Lấy danh sách amenity thuộc property
    // ---------------------
    @Query("""
           SELECT pa
           FROM PropertyAmenity pa
           JOIN FETCH pa.amenity a
           WHERE pa.property.propertyId = :propertyId
           """)
    List<PropertyAmenity> getAmenitiesByPropertyId(@Param("propertyId") int propertyId);


    // ---------------------
    // Lấy danh sách image thuộc property
    // ---------------------
    @Query("""
           SELECT pi
           FROM PropertyImage pi
           WHERE pi.property.propertyId = :propertyId
           """)
    List<PropertyImage> getImagesByPropertyId(@Param("propertyId") int propertyId);
}
