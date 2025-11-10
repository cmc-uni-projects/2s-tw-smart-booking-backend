package com.example.smart_booking_system.repository;

import com.example.smart_booking_system.entity.Property;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PropertyRepository extends JpaRepository<Property, Integer> {
}
