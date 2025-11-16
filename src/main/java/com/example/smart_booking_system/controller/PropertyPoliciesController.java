package com.example.smart_booking_system.controller;


import com.example.smart_booking_system.dto.request.PropertyPoliciesRequestDTO;
import com.example.smart_booking_system.service.PropertyPoliciesService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/policies")
@RequiredArgsConstructor
public class PropertyPoliciesController {

    private final PropertyPoliciesService policyService;

    @PostMapping("/save")
    public ResponseEntity<?> savePolicies(@RequestBody PropertyPoliciesRequestDTO req) {
        try {
            return ResponseEntity.ok(policyService.createOrUpdatePolicies(req));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/property/{propertyId}")
    public ResponseEntity<?> getPolicies(@PathVariable int propertyId) {
        try {
            return ResponseEntity.ok(policyService.getPoliciesByPropertyId(propertyId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
