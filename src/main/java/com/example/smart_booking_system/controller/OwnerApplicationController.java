package com.example.smart_booking_system.controller;

import com.example.smart_booking_system.dto.request.application.OwnerApplicationSubmitDTO;
import com.example.smart_booking_system.dto.response.admin.OwnerApplicationDTO;
import com.example.smart_booking_system.service.OwnerApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/applications")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CUSTOMER')")
public class OwnerApplicationController {

    private final OwnerApplicationService applicationService;

    @PostMapping("/submit-owner")
    public ResponseEntity<OwnerApplicationDTO> submitApplication(
            @Valid @RequestBody OwnerApplicationSubmitDTO submitDTO,
            Authentication authentication) {

        String username = authentication.getName();
        OwnerApplicationDTO submittedApp = applicationService.submitApplication(submitDTO, username);
        return ResponseEntity.ok(submittedApp);
    }
}