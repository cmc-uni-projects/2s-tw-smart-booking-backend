package com.example.smart_booking_system.service;

import com.example.smart_booking_system.entity.Property;
import com.example.smart_booking_system.enums.PropertyStatus;
import com.example.smart_booking_system.repository.PropertyRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class PropertyService {
    private final PropertyRepository propertyRepository;

    public PropertyService(PropertyRepository propertyRepository) {
        this.propertyRepository = propertyRepository;
    }

    public Property addProperty(Property property) {
        // Validate address
        if (property.getAddress() == null || property.getAddress().trim().isEmpty()) {
            throw new IllegalArgumentException("Address cannot be empty");
        }

        // Validate postal code
        if (property.getPostalCode() == null || property.getPostalCode().trim().isEmpty()) {
            throw new IllegalArgumentException("Postal code cannot be empty");
        }

        // Validate latitude
        if (property.getLatitude().compareTo(BigDecimal.valueOf(-90.0)) < 0 ||
                property.getLatitude().compareTo(BigDecimal.valueOf(90.0)) > 0) {
            throw new IllegalArgumentException("Latitude must be between -90 and 90");
        }

        // Validate longitude
        if (property.getLongitude().compareTo(BigDecimal.valueOf(-180.0)) < 0 ||
                property.getLongitude().compareTo(BigDecimal.valueOf(180.0)) > 0) {
            throw new IllegalArgumentException("Longitude must be between -180 and 180");
        }

        // Validate phone number
        if (!property.getPhoneContact().matches("^[0-9]{10}$")) {
            throw new IllegalArgumentException("Phone number must be exactly 10 digits with no special characters");
        }

        // Set default status if not provided
        if (property.getPropertyStatus() == null) {
            property.setPropertyStatus(PropertyStatus.PENDING);
        }

        // Save and return
        return propertyRepository.save(property);
    }

    public List<Property> searchProperties(String city, String keyword) {
        if (city != null && city.trim().isEmpty()) city = null;
        if (keyword != null && keyword.trim().isEmpty()) keyword = null;

        return propertyRepository.searchProperties(city, keyword);
    }


}
