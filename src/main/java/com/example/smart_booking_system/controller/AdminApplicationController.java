package com.example.smart_booking_system.controller;

import com.example.smart_booking_system.dto.request.admin.OwnerApplicationReviewDTO;
import com.example.smart_booking_system.dto.response.ApiResponse;
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
@RequestMapping("/api/v1/admin/owner-applications")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminApplicationController {

    private final OwnerApplicationService ownerApplicationService;

    // 1. API Lấy danh sách đơn (Có lọc theo status)
    // URL: /api/v1/admin/owner-applications?status=PENDING
    @GetMapping
    public ResponseEntity<?> getOwnerApplications(@RequestParam(required = false) String status) {
        try {
            String statusStr = (status != null && !status.isEmpty()) ? status : "PENDING";


            ApplicationStatus appStatus = ApplicationStatus.valueOf(statusStr.toUpperCase());

            List<OwnerApplicationDTO> applications = ownerApplicationService.getApplicationsByStatus(appStatus);

            return ResponseEntity.ok(ApiResponse.success(
                    "Lấy danh sách đơn " + appStatus + " thành công",
                    applications
            ));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Trạng thái không hợp lệ: " + status));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Lỗi server: " + e.getMessage()));
        }
    }

    // 2. API Duyệt/Từ chối đơn
    // URL: /api/v1/admin/owner-applications/{id}/review
    @PostMapping("/{applicationId}/review")
    public ResponseEntity<?> reviewOwnerApplication(
            @PathVariable Long applicationId,
            @Valid @RequestBody OwnerApplicationReviewDTO reviewDTO,
            Authentication authentication
    ) {
        try {
            String adminUsername = authentication.getName();
            OwnerApplicationDTO result = ownerApplicationService.reviewApplication(applicationId, reviewDTO, adminUsername);

            String message;
            if (reviewDTO.getStatus() == ApplicationStatus.APPROVED) {
                message = "Đã duyệt đơn thành công";
            } else if (reviewDTO.getStatus() == ApplicationStatus.REJECTED) {
                message = "Đã từ chối đơn";
            } else {
                message = "Trạng thái không thay đổi";
            }

            return ResponseEntity.ok(ApiResponse.success(message, result));

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Lỗi xử lý: " + e.getMessage()));
        }
    }
}
