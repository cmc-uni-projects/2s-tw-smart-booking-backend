package com.example.smart_booking_system.repository;
import com.example.smart_booking_system.entity.Amenity;
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

    public boolean existsBynameIgnoreCase(String name);

}
