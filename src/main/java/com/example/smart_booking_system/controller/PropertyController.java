package com.example.smart_booking_system.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.ui.Model;
import com.example.smart_booking_system.entity.Property;
import com.example.smart_booking_system.service.PropertyService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/properties")
public class PropertyController {
    private final PropertyService propertyService;
    public PropertyController(PropertyService propertyService) {
        this.propertyService = propertyService;
    }


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
    
}
