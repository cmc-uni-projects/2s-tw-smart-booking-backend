package com.example.smart_booking_system.controller;

import com.example.smart_booking_system.dto.request.admin.OwnerApplicationReviewDTO;
import com.example.smart_booking_system.dto.response.admin.OwnerApplicationDTO;
import com.example.smart_booking_system.enums.ApplicationStatus;
import com.example.smart_booking_system.service.OwnerApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
// ✅ QUAN TRỌNG: Đường dẫn cơ sở là cho "owner-applications"
@RequestMapping("/api/v1/admin/owner-applications")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminApplicationController {

    private final OwnerApplicationService applicationService;
    // Xóa PropertyService vì controller này không xử lý property

    @GetMapping
    public ResponseEntity<List<OwnerApplicationDTO>> getApplications(
            @RequestParam(defaultValue = "PENDING") String status) {

        ApplicationStatus appStatus = ApplicationStatus.valueOf(status.toUpperCase());
        List<OwnerApplicationDTO> applications = applicationService.getApplicationsByStatus(appStatus);
        return ResponseEntity.ok(applications);
    }

    // Đường dẫn đầy đủ: /api/v1/admin/owner-applications/{applicationId}/review
    @PostMapping("/{applicationId}/review")
    public ResponseEntity<OwnerApplicationDTO> reviewApplication(
            @PathVariable Long applicationId,
            @Valid @RequestBody OwnerApplicationReviewDTO reviewDTO,
            Authentication authentication) {

        String adminUsername = authentication.getName();
        OwnerApplicationDTO reviewedApp = applicationService.reviewApplication(applicationId, reviewDTO, adminUsername);
        return ResponseEntity.ok(reviewedApp);
    }

    // ✅ Đã xóa 2 hàm getPendingProperties() và reviewProperty() khỏi file này
}