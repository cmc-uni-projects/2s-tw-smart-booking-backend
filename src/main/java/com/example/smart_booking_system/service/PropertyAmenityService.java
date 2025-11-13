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


    public PropertyAmenityDTO toDTO(PropertyAmenity pa) {
        return PropertyAmenityDTO.builder()
                .propertyAmenityId(pa.getPropertyAmenityId())
                .propertyId(pa.getPropertyId().getPropertyId())
                .amenityId(pa.getAmenityId().getAmenityId())
                .amenityName(pa.getAmenityId().getAmenityName())
                .build();
    }


    public PropertyAmenity addPropertyAmenity(int propertyId, int amenityId) {

        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new IllegalArgumentException("Property not found"));

        Amenity amenity = amenityRepository.findById(amenityId)
                .orElseThrow(() -> new IllegalArgumentException("Amenity not found"));


        if (amenity.getAmenityType() != AmenityType.PROPERTY) {
            throw new IllegalArgumentException("This amenity is not a PROPERTY amenity");
        }


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

    public PropertyAmenityDTO updatePropertyAmenity(int id, int newAmenityId) {

        PropertyAmenity pa = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("PropertyAmenity not found"));

        Amenity newAmenity = amenityRepository.findById(newAmenityId)
                .orElseThrow(() -> new IllegalArgumentException("Amenity not found"));

        if (newAmenity.getAmenityType() != AmenityType.PROPERTY) {
            throw new IllegalArgumentException("This amenity is not a PROPERTY amenity");
        }

        int propertyId = pa.getPropertyId().getPropertyId();

        if (repo.existsActiveExcept(propertyId, newAmenityId, id)) {
            throw new IllegalArgumentException("This amenity is already used for this property");
        }

        pa.setAmenityId(newAmenity);
        PropertyAmenity saved = repo.save(pa);

        return toDTO(saved);
    }

    public String deletePropertyAmenity(int id) {
        PropertyAmenity pa = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("PropertyAmenity not found"));

        pa.setActive(false);
        repo.save(pa);

        return "PropertyAmenity deleted successfully.";
    }
}