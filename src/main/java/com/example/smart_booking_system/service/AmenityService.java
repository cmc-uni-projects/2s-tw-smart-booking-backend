package com.example.smart_booking_system.service;

import com.example.smart_booking_system.exception.ResourceNotFoundException;
import com.example.smart_booking_system.exception.BadRequestException;
import com.example.smart_booking_system.entity.Amenity;
import com.example.smart_booking_system.enums.AmenityType;
import com.example.smart_booking_system.repository.AmenityRepository;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
public class AmenityService {

    private final AmenityRepository amenityRepository;

    public AmenityService(AmenityRepository amenityRepository) {
        this.amenityRepository = amenityRepository;
    }

    // ADD
    public Amenity addAmenity(Amenity amenity) {

        if (amenityRepository.existsByNameAndType(
                amenity.getAmenityName(),
                amenity.getAmenityType()
        )) {
            throw new BadRequestException("error.amenity.duplicate");
        }

        return amenityRepository.save(amenity);
    }

    // GET ALL ACTIVE
    public List<Amenity> findAll() {
        return amenityRepository.findAllActive();
    }

    // GET BY TYPE
    public List<Amenity> findByType(AmenityType type) {
        return amenityRepository.findByType(type);
    }

    // UPDATE
    public Amenity updateAmenity(int id, Amenity updatedAmenity) {

        Amenity existing = amenityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("error.amenity.notFound", id));

        // Lấy giá trị mới hoặc giữ nguyên
        String newName = updatedAmenity.getAmenityName() != null && !updatedAmenity.getAmenityName().trim().isEmpty()
                ? updatedAmenity.getAmenityName()
                : existing.getAmenityName();

        AmenityType newType = updatedAmenity.getAmenityType() != null
                ? updatedAmenity.getAmenityType()
                : existing.getAmenityType();

        // Validate type
        if (updatedAmenity.getAmenityType() != null) {
            boolean validType = Arrays.stream(AmenityType.values())
                    .anyMatch(t -> t.equals(updatedAmenity.getAmenityType()));
            if (!validType) {
                throw new BadRequestException("error.amenity.invalidType", updatedAmenity.getAmenityType());
            }
        }

        // Check trùng bản ghi khác
        boolean exists = amenityRepository.existsByNameTypeExcept(newName, newType, id);
        if (exists) {
            throw new BadRequestException("error.amenity.duplicate");
        }

        // Update
        existing.setAmenityName(newName);
        existing.setAmenityType(newType);
        existing.setActive(updatedAmenity.isActive());

        return amenityRepository.save(existing);
    }

    // DELETE (SOFT)
    public void deactivateAmenity(int id) {
        Amenity amenity = amenityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("error.amenity.notFound", id));

        if (!amenity.isActive()) {
            throw new BadRequestException("error.amenity.alreadyInactive");
        }

        amenity.setActive(false);
        amenityRepository.save(amenity);
    }
}
