package com.example.smart_booking_system.service;

import com.example.smart_booking_system.entity.Amenity;
import com.example.smart_booking_system.repository.AmenityRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AmenityService {
    private final AmenityRepository amenityRepository;

    public AmenityService(AmenityRepository amenityRepository) {
        this.amenityRepository = amenityRepository;
    }

    public Amenity addAmenity(Amenity amenity){
        if (amenityRepository.existsBynameIgnoreCase(amenity.getAmenityName())){
            throw new RuntimeException("Amenity already exists");
        }

        return amenityRepository.save(amenity);
    }

    public List<Amenity> findAll(){
        return amenityRepository.findAllActive();
    }

}
