package com.example.smart_booking_system.service;

import com.example.smart_booking_system.dto.PropertyAmenityDTO;
import com.example.smart_booking_system.entity.Amenity;
import com.example.smart_booking_system.entity.Property;
import com.example.smart_booking_system.entity.PropertyAmenity;
import com.example.smart_booking_system.enums.AmenityType;
import com.example.smart_booking_system.repository.AmenityRepository;
import com.example.smart_booking_system.repository.PropertyAmenityRepository;
import com.example.smart_booking_system.repository.PropertyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PropertyAmenityService {

    private final PropertyAmenityRepository repo;
    private final PropertyRepository propertyRepository;
    private final AmenityRepository amenityRepository;

    // Convert to DTO
    public PropertyAmenityDTO toDTO(PropertyAmenity pa) {
        return PropertyAmenityDTO.builder()
                .propertyAmenityId(pa.getPropertyAmenityId())
                .propertyId(pa.getPropertyId().getPropertyId())
                .amenityId(pa.getAmenityId().getAmenityId())
                .amenityName(pa.getAmenityId().getAmenityName())
                .build();
    }

    // ---------------- ADD ----------------
    public PropertyAmenity addPropertyAmenity(int propertyId, int amenityId) {

        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new IllegalArgumentException("Property not found"));

        Amenity amenity = amenityRepository.findById(amenityId)
                .orElseThrow(() -> new IllegalArgumentException("Amenity not found"));

        // CHECK: amenityType MUST BE PROPERTY
        if (amenity.getAmenityType() != AmenityType.PROPERTY) {
            throw new IllegalArgumentException("This amenity is not a PROPERTY amenity");
        }

        // CHECK DUPLICATE
        if (repo.existsActive(propertyId, amenityId)) {
            throw new IllegalArgumentException("Amenity already added to this property");
        }

        PropertyAmenity pa = new PropertyAmenity();
        pa.setPropertyId(property);
        pa.setAmenityId(amenity);
        pa.setActive(true);

        return repo.save(pa);
    }
    public List<PropertyAmenityDTO> getAmenitiesByProperty(int propertyId) {
        return repo.findActiveByPropertyId(propertyId)
                .stream()
                .map(this::toDTO)
                .toList();
    }
}