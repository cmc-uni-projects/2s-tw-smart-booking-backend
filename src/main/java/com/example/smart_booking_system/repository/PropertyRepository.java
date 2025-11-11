package com.example.smart_booking_system.repository;

import com.example.smart_booking_system.entity.Property;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PropertyRepository extends JpaRepository<Property, Integer> {

    @Query("""
        SELECT p FROM Property p
        WHERE 
        (:city IS NULL OR LOWER(p.city) LIKE LOWER(CONCAT('%', :city, '%')))
        AND
            (:keyword IS NULL OR (
            LOWER(p.propertyName) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(p.propertyType) LIKE LOWER(CONCAT('%', :keyword, '%'))
            ))
            AND p.isActive = true
            AND p.propertyStatus = 'APPROVE'
    """)
    List<Property> searchProperties(@Param("city") String city, @Param("keyword") String keyword);
}
