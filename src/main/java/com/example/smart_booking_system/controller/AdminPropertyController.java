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
@RequestMapping("/api/v1/admin/properties")
@PreAuthorize("hasRole('ADMIN')")
public class AdminPropertyController {

    private final PropertyService propertyService;

    // ✅ THÊM MỚI: API Lấy danh sách theo trạng thái (Dynamic)
    // URL gọi: /api/v1/admin/properties/status?status=PENDING (hoặc APPROVE, REJECTED)
    @GetMapping("/status")
    public ResponseEntity<?> getPropertiesByStatus(@RequestParam String status) {
        try {
            // 1. Convert String sang Enum (Nếu sai tên sẽ nhảy xuống catch IllegalArgumentException)
            PropertyStatus propertyStatus = PropertyStatus.valueOf(status.toUpperCase());

            // 2. Gọi Service lấy danh sách
            List<PropertyDetailDTO> properties = propertyService.getPropertiesByStatus(propertyStatus);

            return ResponseEntity.ok(properties); // Trả về List trực tiếp để khớp với FE

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Trạng thái không hợp lệ: " + status));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Lỗi server: " + e.getMessage()));
        }
    }

    /**
     * [ADMIN] Duyệt (Approve) hoặc Từ chối (Reject)
     */
    @PostMapping("/{propertyId}/review")
    public ResponseEntity<?> reviewProperty(
            @PathVariable Integer propertyId,
            @Valid @RequestBody PropertyReviewDTO reviewDTO,
            Authentication authentication
    ) {
        try {
            String adminUsername = (authentication != null) ? authentication.getName() : "Admin";

            PropertyDetailDTO reviewedProperty = propertyService.reviewProperty(propertyId, reviewDTO, adminUsername);

            String message = (reviewDTO.getStatus() == PropertyStatus.APPROVE)
                    ? "Duyệt cơ sở thành công"
                    : "Từ chối cơ sở thành công";

            return ResponseEntity.ok(ApiResponse.success(message, reviewedProperty));

        } catch (ResourceNotFoundException | IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Lỗi server: " + e.getMessage()));
        }
    }
}