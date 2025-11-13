package com.example.smart_booking_system.service;

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

    public Amenity addAmenity(Amenity amenity){
        if (amenityRepository.existsBynameIgnoreCase(amenity.getAmenityName())){
            throw new IllegalArgumentException("Amenity already exists");
        }

        return amenityRepository.save(amenity);
    }

    public List<Amenity> findAll(){
        return amenityRepository.findAllActive();
    }

    
    public Amenity updateAmenity(int id, Amenity updatedAmenity) {

        Amenity existingAmenity = amenityRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Amenity not found with id: " + id));

        if (updatedAmenity.getAmenityName() != null && !updatedAmenity.getAmenityName().trim().isEmpty()) {
            boolean nameExists = amenityRepository.existsBynameIgnoreCase(updatedAmenity.getAmenityName());

            if (!updatedAmenity.getAmenityName().equalsIgnoreCase(existingAmenity.getAmenityName())
                    && nameExists) {
                throw new IllegalArgumentException("Amenity name already exists");
            }
            existingAmenity.setAmenityName(updatedAmenity.getAmenityName());
        }

        if (updatedAmenity.getAmenityType() != null) {
            boolean validType = Arrays.stream(AmenityType.values())
                    .anyMatch(t -> t.equals(updatedAmenity.getAmenityType()));

            if (!validType) {
                throw new IllegalArgumentException("Invalid amenity type: " + updatedAmenity.getAmenityType());
            }

            existingAmenity.setAmenityType(updatedAmenity.getAmenityType());
        }
        existingAmenity.setActive(updatedAmenity.isActive());

        return amenityRepository.save(existingAmenity);
    }

    public List<Amenity> findByType(AmenityType type) {
        return amenityRepository.findByType(type);
    }

    public void deactivateAmenity(int id) {
        Amenity amenity = amenityRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Amenity not found with id: " + id));

        if (!amenity.isActive()) {
            throw new IllegalArgumentException("Amenity is already inactive");
        }

        amenity.setActive(false);
        amenityRepository.save(amenity);
    }


}
