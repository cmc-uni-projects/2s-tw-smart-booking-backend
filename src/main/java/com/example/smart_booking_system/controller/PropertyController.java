package com.example.smart_booking_system.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import com.example.smart_booking_system.entity.Property;
import com.example.smart_booking_system.service.PropertyService;
import org.springframework.web.bind.annotation.*;
import com.example.smart_booking_system.dto.response.property.FeaturedPropertyDTO;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/properties")
public class PropertyController {
    private final PropertyService propertyService;

    @PostMapping("/add")
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN')")
    public ResponseEntity<?> addProperty(@RequestBody Property property) {
        try {
            Property savedProperty = propertyService.addProperty(property);

            return ResponseEntity
                    .status(201)
                    .body(savedProperty);
        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .badRequest()
                    .body(e.getMessage());

        } catch (Exception e) {
            return ResponseEntity
                    .internalServerError()
                    .body("Error adding property: " + e.getMessage());
        }
    }

    @GetMapping("/search")
    public ResponseEntity<List<Property>> searchProperties(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String keyword) {

        return ResponseEntity.ok(propertyService.searchProperties(city, keyword));
    }

    @PutMapping("/update/{id}")
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN')")
    public ResponseEntity<?> updateProperty(
            @PathVariable int id,
            @RequestBody Property updatedProperty
    ) {
        try {
            Property property = propertyService.updateProperty(id, updatedProperty);
            return ResponseEntity.ok(property);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error updating property: " + e.getMessage());
        }
    }

    @GetMapping("/featured")
    public ResponseEntity<List<FeaturedPropertyDTO>> getFeaturedProperties() {
        List<FeaturedPropertyDTO> properties = propertyService.getFeaturedProperties();
        return ResponseEntity.ok(properties);
    }
}
