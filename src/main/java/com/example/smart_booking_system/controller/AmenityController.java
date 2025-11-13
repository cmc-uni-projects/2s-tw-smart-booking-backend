package com.example.smart_booking_system.controller;


import com.example.smart_booking_system.entity.Amenity;
import com.example.smart_booking_system.service.AmenityService;
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
    public Amenity addAmenity(@RequestBody Amenity amenity){
        try {
            Amenity newAmenity = amenityService.addAmenity(amenity);
        }
    }
}
