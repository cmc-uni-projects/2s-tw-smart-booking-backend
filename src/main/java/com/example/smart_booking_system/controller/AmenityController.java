package com.example.smart_booking_system.controller;


import com.example.smart_booking_system.entity.Amenity;
import com.example.smart_booking_system.service.AmenityService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/amenity")
public class AmenityController {
    private final AmenityService amenityService;

    public AmenityController(AmenityService amenityService) {
        this.amenityService = amenityService;
    }
    @GetMapping("/search")
    public List<Amenity> getAll(){
        return amenityService.findAll();
    }

    @PostMapping("/add")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> addAmenity(@RequestBody Amenity amenity) {
        try {
            Amenity savedAmenity = amenityService.addAmenity(amenity);

            return ResponseEntity
                    .status(201)
                    .body(savedAmenity);

        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .badRequest()
                    .body(e.getMessage());

        } catch (Exception e) {
            return ResponseEntity
                    .internalServerError()
                    .body("Error adding amenity: " + e.getMessage());
        }
    }
}
