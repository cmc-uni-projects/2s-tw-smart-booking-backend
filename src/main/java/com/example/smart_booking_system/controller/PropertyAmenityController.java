package com.example.smart_booking_system.controller;

import com.example.smart_booking_system.service.PropertyAmenityService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/propertyAmenity")
@RequiredArgsConstructor
public class PropertyAmenityController {

    private final PropertyAmenityService service;

    @PostMapping("/add")
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN')")
    public ResponseEntity<?> add(
            @RequestParam int propertyId,
            @RequestParam int amenityId) {

        try {
            return ResponseEntity.status(201)
                    .body(service.toDTO(service.addPropertyAmenity(propertyId, amenityId)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}