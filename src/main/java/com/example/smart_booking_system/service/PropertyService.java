package com.example.smart_booking_system.service;

import com.example.smart_booking_system.entity.Property;
import com.example.smart_booking_system.enums.PropertyStatus;
import com.example.smart_booking_system.repository.PropertyRepository;
import org.springframework.stereotype.Service;
import com.example.smart_booking_system.dto.response.property.FeaturedPropertyDTO;
import java.util.stream.Collectors;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PropertyService {
    private final PropertyRepository propertyRepository;
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

    public Property updateProperty(int id, Property updatedProperty) {
        Property existingProperty = propertyRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Property not found with id: " + id));

        if (updatedProperty.getPropertyName() != null &&
                !updatedProperty.getPropertyName().equals(existingProperty.getPropertyName())) {
            existingProperty.setPropertyName(updatedProperty.getPropertyName());
        }

        if (updatedProperty.getAddress() != null &&
                !updatedProperty.getAddress().equals(existingProperty.getAddress())) {
            existingProperty.setAddress(updatedProperty.getAddress());
        }

        if (updatedProperty.getCity() != null &&
                !updatedProperty.getCity().equals(existingProperty.getCity())) {
            existingProperty.setCity(updatedProperty.getCity());
        }

        if (updatedProperty.getCountry() != null &&
                !updatedProperty.getCountry().equals(existingProperty.getCountry())) {
            existingProperty.setCountry(updatedProperty.getCountry());
        }

        if (updatedProperty.getPostalCode() != null &&
                !updatedProperty.getPostalCode().equals(existingProperty.getPostalCode())) {
            existingProperty.setPostalCode(updatedProperty.getPostalCode());
        }

        if (updatedProperty.getDescription() != null &&
                !updatedProperty.getDescription().equals(existingProperty.getDescription())) {
            existingProperty.setDescription(updatedProperty.getDescription());
        }

        if (updatedProperty.getLatitude() != null &&
                !updatedProperty.getLatitude().equals(existingProperty.getLatitude())) {
            existingProperty.setLatitude(updatedProperty.getLatitude());
        }

        if (updatedProperty.getLongitude() != null &&
                !updatedProperty.getLongitude().equals(existingProperty.getLongitude())) {
            existingProperty.setLongitude(updatedProperty.getLongitude());
        }

        if (updatedProperty.getPhoneContact() != null &&
                !updatedProperty.getPhoneContact().equals(existingProperty.getPhoneContact())) {
            existingProperty.setPhoneContact(updatedProperty.getPhoneContact());
        }

        if (updatedProperty.getEmailContact() != null &&
                !updatedProperty.getEmailContact().equals(existingProperty.getEmailContact())) {
            existingProperty.setEmailContact(updatedProperty.getEmailContact());
        }

        if (updatedProperty.getPropertyType() != null &&
                !updatedProperty.getPropertyType().equals(existingProperty.getPropertyType())) {
            existingProperty.setPropertyType(updatedProperty.getPropertyType());
        }

        if (updatedProperty.isActive() != existingProperty.isActive()) {
            existingProperty.setActive(updatedProperty.isActive());
        }

        existingProperty.setUpdatedAt(LocalDate.now());

        return propertyRepository.save(existingProperty);
    }


    public List<FeaturedPropertyDTO> getFeaturedProperties() {

        List<Property> properties = propertyRepository.findFeaturedProperties();

        return properties.stream()
                .map(this::convertToFeaturedDTO)
                .collect(Collectors.toList());
    }

    private FeaturedPropertyDTO convertToFeaturedDTO(Property property) {
        FeaturedPropertyDTO dto = new FeaturedPropertyDTO();
        dto.setPropertyId(property.getPropertId());
        dto.setPropertyName(property.getPropertyName());
        dto.setCity(property.getCity());
        dto.setRating(property.getRating());
        dto.setReviewCount(property.getReviewCount());
        return dto;
    }


}
