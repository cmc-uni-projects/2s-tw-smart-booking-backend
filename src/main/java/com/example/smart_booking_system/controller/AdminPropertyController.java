package com.example.smart_booking_system.controller;

import com.example.smart_booking_system.dto.request.admin.PropertyReviewDTO;
import com.example.smart_booking_system.dto.response.ApiResponse;
import com.example.smart_booking_system.dto.response.property.PropertyDetailDTO;
import com.example.smart_booking_system.enums.PropertyStatus;
import com.example.smart_booking_system.exception.ResourceNotFoundException;
import com.example.smart_booking_system.service.PropertyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
// ✅ QUAN TRỌNG: Đường dẫn cơ sở là cho "properties"
@RequestMapping("/api/v1/admin/properties")
@PreAuthorize("hasRole('ADMIN')")
public class AdminPropertyController {

    private final PropertyService propertyService;
    // Xóa OwnerApplicationService

    /**
     * [ADMIN] Lấy danh sách cơ sở đang chờ duyệt (PENDING)
     * Đường dẫn đầy đủ: /api/v1/admin/properties/pending
     */
    @GetMapping("/pending")
    public ResponseEntity<?> getPendingProperties() {
        try {
            List<PropertyDetailDTO> properties = propertyService.getPropertiesByStatus(PropertyStatus.PENDING);
            return ResponseEntity.ok(ApiResponse.success(properties.size() + " cơ sở đang chờ duyệt", properties));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * [ADMIN] Duyệt (Approve) hoặc Từ chối (Reject) một cơ sở
     * Đường dẫn đầy đủ: /api/v1/admin/properties/{propertyId}/review
     */
    @PostMapping("/{propertyId}/review")
    public ResponseEntity<?> reviewProperty(
            @PathVariable Integer propertyId,
            @Valid @RequestBody PropertyReviewDTO reviewDTO,
            Authentication authentication
    ) {
        try {
            String adminUsername = authentication.getName();
            PropertyDetailDTO reviewedProperty = propertyService.reviewProperty(propertyId, reviewDTO, adminUsername);

            String message = reviewDTO.getStatus().equalsIgnoreCase("APPROVE")
                    ? "Duyệt cơ sở thành công"
                    : "Từ chối cơ sở thành công";

            return ResponseEntity.ok(ApiResponse.success(message, reviewedProperty));

        } catch (ResourceNotFoundException | IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error("Lỗi server: " + e.getMessage()));
        }
    }
}