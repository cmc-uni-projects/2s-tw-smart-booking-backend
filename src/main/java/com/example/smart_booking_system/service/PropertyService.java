package com.example.smart_booking_system.service;

import com.example.smart_booking_system.entity.Property;
import com.example.smart_booking_system.repository.PropertyRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class PropertyService {
    private final PropertyRepository propertyRepository;

    public PropertyService(PropertyRepository propertyRepository) {
        this.propertyRepository = propertyRepository;
    }

    public void addProperty(Property property) {
        if (property.getAddress() == null || property.getAddress().trim().isEmpty()){
            throw new IllegalArgumentException("address cannot be empty");
        }

        if (property.getPostalCode() == null || property.getPostalCode().trim().isEmpty()){
            throw new IllegalArgumentException("postal code cannot be empty");
        }
        if (property.getLatitude().compareTo(BigDecimal.valueOf(-90.0)) < 0 ||
                property.getLatitude().compareTo(BigDecimal.valueOf(90.0)) > 0) {
            throw new IllegalArgumentException("latitude must be between 90 and 90.");
        }
        if (property.getLongitude().compareTo(BigDecimal.valueOf(-180.0)) < 0 ||
                property.getLongitude().compareTo(BigDecimal.valueOf(180.0)) > 0) {
            throw new IllegalArgumentException("longitude must be between 180 and 180.");
        }
        if (!property.getPhoneContact().matches("^[0-9]{10}$")) {
            throw new IllegalArgumentException("Phone number must be exactly 10 digits with no special characters");
        }
    }
}
