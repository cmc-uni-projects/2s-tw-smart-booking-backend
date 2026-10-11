package com.example.smart_booking_system.service;

import com.example.smart_booking_system.exception.ResourceNotFoundException;
import com.example.smart_booking_system.exception.BadRequestException;
import com.example.smart_booking_system.dto.PropertyAmenityResponseDTO;
import com.example.smart_booking_system.entity.Amenity;
import com.example.smart_booking_system.entity.Property;
import com.example.smart_booking_system.entity.PropertyAmenity;
import com.example.smart_booking_system.enums.AmenityType;
import com.example.smart_booking_system.repository.AmenityRepository;
import com.example.smart_booking_system.repository.PropertyAmenityRepository;
import com.example.smart_booking_system.repository.PropertyRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PropertyAmenityService {

    private final PropertyRepository propertyRepository;
    private final AmenityRepository amenityRepository;
    private final PropertyAmenityRepository propertyAmenityRepository;

    // =========================================
    // 1) ADD MULTIPLE
    // =========================================
    @Transactional
    public List<PropertyAmenityResponseDTO> addMultiplePropertyAmenity(
            int propertyId,
            List<Integer> amenityIds
    ) {

        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new ResourceNotFoundException("error.property.notFound"));

        // Validate toàn bộ trước khi thêm
        for (Integer amenityId : amenityIds) {

            Amenity amenity = amenityRepository.findById(amenityId)
                    .orElseThrow(() -> new ResourceNotFoundException("error.amenity.notFound", amenityId));

            if (amenity.getAmenityType() != AmenityType.PROPERTY) {
                throw new BadRequestException("error.amenity.wrongType", amenityId);
            }

            boolean exists = propertyAmenityRepository
                    .existsByPropertyAndAmenity(propertyId, amenityId);

            if (exists) {
                throw new BadRequestException("error.amenity.alreadyAdded", amenityId);
            }
        }

        // Nếu tất cả hợp lệ → tiến hành add
        List<PropertyAmenity> savedList = new ArrayList<>();

        for (Integer amenityId : amenityIds) {
            Amenity amenity = amenityRepository.findById(amenityId).get();

            PropertyAmenity pa = new PropertyAmenity();
            pa.setProperty(property);
            pa.setAmenity(amenity);
            pa.setActive(true);

            savedList.add(propertyAmenityRepository.save(pa));
        }

        // Convert sang DTO
        return savedList.stream()
                .map(pa -> new PropertyAmenityResponseDTO(
                        pa.getPropertyAmenityId(),
                        propertyId,
                        pa.getAmenity().getAmenityId(),
                        pa.getAmenity().getAmenityName()
                ))
                .toList();
    }

    // =========================================
    // 2) READ BY PROPERTY
    // =========================================
    public List<PropertyAmenityResponseDTO> getByPropertyId(int propertyId) {

        List<PropertyAmenity> list =
                propertyAmenityRepository.findActiveByProperty(propertyId);

        return list.stream()
                .map(pa -> new PropertyAmenityResponseDTO(
                        pa.getPropertyAmenityId(),
                        pa.getProperty().getPropertyId(),
                        pa.getAmenity().getAmenityId(),
                        pa.getAmenity().getAmenityName()
                ))
                .toList();
    }

    // =========================================
    // 3) UPDATE (theo AmenityId cũ)
    // =========================================
    @Transactional
    public PropertyAmenityResponseDTO updatePropertyAmenity(
            int propertyId,
            int oldAmenityId,
            int newAmenityId
    ) {

        PropertyAmenity existing =
                propertyAmenityRepository.findByPropertyAndAmenity(propertyId, oldAmenityId);

        if (existing == null) {
            throw new BadRequestException("error.amenity.notLinked");
        }

        Amenity newAmenity = amenityRepository.findById(newAmenityId)
                .orElseThrow(() -> new ResourceNotFoundException("error.amenity.notFound"));

        if (newAmenity.getAmenityType() != AmenityType.PROPERTY) {
            throw new BadRequestException("error.amenity.wrongType");
        }

        // Check duplicate
        boolean alreadyAssigned =
                propertyAmenityRepository.findByPropertyAndAmenity(propertyId, newAmenityId) != null;

        if (alreadyAssigned) {
            throw new BadRequestException("error.amenity.alreadyAdded");
        }

        existing.setAmenity(newAmenity);
        PropertyAmenity saved = propertyAmenityRepository.save(existing);

        return new PropertyAmenityResponseDTO(
                saved.getPropertyAmenityId(),
                propertyId,
                saved.getAmenity().getAmenityId(),
                saved.getAmenity().getAmenityName()
        );
    }

    // =========================================
    // 4) DELETE
    // =========================================
    @Transactional
    public String deletePropertyAmenity(int propertyId, int amenityId) {

        PropertyAmenity existing =
                propertyAmenityRepository.findByPropertyAndAmenity(propertyId, amenityId);

        if (existing == null) {
            throw new BadRequestException("error.amenity.notLinked");
        }

        existing.setActive(false);
        propertyAmenityRepository.save(existing);

        return "Deleted successfully";
    }
}
