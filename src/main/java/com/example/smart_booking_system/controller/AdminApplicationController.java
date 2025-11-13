package com.example.smart_booking_system.controller;

import com.example.smart_booking_system.dto.request.admin.OwnerApplicationReviewDTO;
import com.example.smart_booking_system.dto.response.admin.OwnerApplicationDTO;
import com.example.smart_booking_system.enums.ApplicationStatus;
import com.example.smart_booking_system.service.OwnerApplicationService;
import com.example.smart_booking_system.service.PropertyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import com.example.smart_booking_system.dto.request.admin.PropertyReviewDTO;
import com.example.smart_booking_system.enums.PropertyStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import com.example.smart_booking_system.dto.response.property.PropertyDetailDTO;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminApplicationController {

    private final OwnerApplicationService applicationService;
    private final PropertyService propertyService;
    @GetMapping("/owner-applications")
    public ResponseEntity<List<OwnerApplicationDTO>> getApplications(
            @RequestParam(defaultValue = "PENDING") String status) {

        ApplicationStatus appStatus = ApplicationStatus.valueOf(status.toUpperCase());
        List<OwnerApplicationDTO> applications = applicationService.getApplicationsByStatus(appStatus);
        return ResponseEntity.ok(applications);
    }

    @PostMapping("/owner-applications/{applicationId}/review")
    public ResponseEntity<OwnerApplicationDTO> reviewApplication(
            @PathVariable Long applicationId,
            @Valid @RequestBody OwnerApplicationReviewDTO reviewDTO,
            Authentication authentication) {

        String adminUsername = authentication.getName();
        OwnerApplicationDTO reviewedApp = applicationService.reviewApplication(applicationId, reviewDTO, adminUsername);
        return ResponseEntity.ok(reviewedApp);
    }

    @GetMapping("/properties/pending")
    public ResponseEntity<List<PropertyDetailDTO>> getPendingProperties() {


        List<PropertyDetailDTO> propertyDTOs = propertyService.getPropertiesByStatus(PropertyStatus.PENDING);
        return ResponseEntity.ok(propertyDTOs);
    }

    @PostMapping("/properties/{propertyId}/review")
    public ResponseEntity<PropertyDetailDTO> reviewProperty(
        @PathVariable Integer propertyId,
        @Valid @RequestBody PropertyReviewDTO reviewDTO,
        Authentication authentication) {

        String adminUsername = authentication.getName();

        PropertyDetailDTO reviewedPropertyDTO = propertyService.reviewProperty(propertyId, reviewDTO, adminUsername);
        return ResponseEntity.ok(reviewedPropertyDTO);
    }
}