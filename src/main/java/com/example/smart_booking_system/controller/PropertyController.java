package com.example.smart_booking_system.controller;

import com.example.smart_booking_system.service.PropertyService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import com.example.smart_booking_system.entity.Property;
import org.springframework.web.bind.annotation.*;
import com.example.smart_booking_system.dto.response.property.PropertyDetailDTO;
import org.springframework.security.core.Authentication;
import com.example.smart_booking_system.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;

// --- Imports cho các hàm mới ---
import com.example.smart_booking_system.dto.request.property.PropertyApplicationSubmitDTO;
import com.example.smart_booking_system.dto.response.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;
import com.example.smart_booking_system.enums.PropertyStatus;
import com.example.smart_booking_system.dto.request.admin.PropertyReviewDTO;
import com.example.smart_booking_system.exception.ResourceNotFoundException;
import jakarta.validation.Valid;
// --- Kết thúc Imports ---

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/properties")
public class PropertyController {
    private final PropertyService propertyService;

    @PostMapping("/add")
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN')")
    public ResponseEntity<?> addProperty(@RequestBody Property property, Authentication authentication) {
        try {
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            String ownerId = userDetails.getUserId();

            Property savedProperty = propertyService.addProperty(property, ownerId);

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

    @PostMapping("/submit-application")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<?> submitPropertyApplication(
            @RequestPart("propertyData") String propertyDataJson,
            @RequestPart("propertyImages") List<MultipartFile> propertyImages,
            Authentication authentication
    ) {
        try {
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            String ownerId = userDetails.getUserId();

            // Dùng ObjectMapper để chuyển đổi String JSON thành DTO
            ObjectMapper objectMapper = new ObjectMapper();
            PropertyApplicationSubmitDTO dto = objectMapper.readValue(propertyDataJson, PropertyApplicationSubmitDTO.class);

            // Gọi Service
            PropertyDetailDTO newProperty = propertyService.submitPropertyApplication(dto, propertyImages, ownerId);

            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Nộp đơn đăng ký cơ sở thành công", newProperty));

        } catch (IllegalArgumentException | com.fasterxml.jackson.core.JsonProcessingException e) {
            // Lỗi từ DTO validation hoặc JSON parse
            return ResponseEntity
                    .badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity
                    .internalServerError()
                    .body(ApiResponse.error("Lỗi khi nộp đơn: " + e.getMessage()));
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
    public ResponseEntity<List<PropertyDetailDTO>> getFeaturedProperties() {
        List<PropertyDetailDTO> properties = propertyService.getFeaturedProperties();
        return ResponseEntity.ok(properties);
    }

    // =================================================================
    // === 2 ENDPOINT MỚI CHO ADMIN DUYỆT CƠ SỞ (Hotels Submissions) ===
    // =================================================================

    /**
     * [ADMIN] Lấy danh sách cơ sở theo trạng thái (VD: PENDING)
     */
    @GetMapping("/status/{status}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getPropertiesByStatus(@PathVariable String status) {
        try {
            // Chuyển string "pending" thành Enum PropertyStatus.PENDING
            PropertyStatus propertyStatus = PropertyStatus.valueOf(status.toUpperCase());

            List<PropertyDetailDTO> properties = propertyService.getPropertiesByStatus(propertyStatus);

            return ResponseEntity.ok(ApiResponse.success(properties.size() + " cơ sở được tìm thấy", properties));

        } catch (IllegalArgumentException e) {
            // Bắt lỗi nếu status không hợp lệ (VD: "peending" thay vì "pending")
            return ResponseEntity.badRequest().body(ApiResponse.error("Trạng thái không hợp lệ: " + status));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * [ADMIN] Duyệt (Approve) hoặc Từ chối (Reject) một cơ sở
     */
    @PostMapping("/{propertyId}/review")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> reviewProperty(
            @PathVariable Integer propertyId,
            @Valid @RequestBody PropertyReviewDTO reviewDTO, // DTO chứa { status, reason }
            Authentication authentication
    ) {
        try {
            // Lấy email của Admin đang đăng nhập (chính là username)
            String adminUsername = authentication.getName();

            PropertyDetailDTO reviewedProperty = propertyService.reviewProperty(propertyId, reviewDTO, adminUsername);

            String message = reviewDTO.getStatus().equalsIgnoreCase("APPROVE")
                    ? "Duyệt cơ sở thành công"
                    : "Từ chối cơ sở thành công";

            return ResponseEntity.ok(ApiResponse.success(message, reviewedProperty));

        } catch (ResourceNotFoundException | IllegalStateException e) {
            // Bắt lỗi nếu Property không tìm thấy, hoặc đã được duyệt/từ chối trước đó
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error("Lỗi server: " + e.getMessage()));
        }
    }
}