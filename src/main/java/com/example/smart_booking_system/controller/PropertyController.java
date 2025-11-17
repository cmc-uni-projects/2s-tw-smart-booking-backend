package com.example.smart_booking_system.controller;

import com.example.smart_booking_system.service.PropertyService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import com.example.smart_booking_system.service.SearchHistoryService;
import org.springframework.security.core.context.SecurityContextHolder;
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
// --- Kết thúc Imports ---

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/properties")
public class PropertyController {
    private final PropertyService propertyService;
    private final SearchHistoryService searchHistoryService;

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
            @RequestParam(required = false) String keyword,
            Authentication authentication // <-- NHẬN VÀO ĐỐI TƯỢNG AUTHENTICATION
    ) {

        // 1. Xử lý lưu lịch sử tìm kiếm
        if (authentication != null && authentication.isAuthenticated()) {
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            String userId = userDetails.getUserId();

            String searchTerm = (keyword != null) ? keyword : city;

            if (searchTerm != null && !searchTerm.isBlank()) {
                searchHistoryService.saveSearchHistory(userId, searchTerm);
            }
        }

        return ResponseEntity.ok(propertyService.searchProperties(city, keyword));
    }
    @PutMapping("/update/{id}")
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN')")
    public ResponseEntity<?> updateProperty(
            @PathVariable int id,
            @RequestBody Property updatedProperty
    ) {
        try {
            PropertyDetailDTO property = propertyService.updateProperty(id, updatedProperty);
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

    @GetMapping("/my-properties")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<?> getMyProperties(Authentication authentication) {
        try {
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            String ownerId = userDetails.getUserId();

            List<PropertyDetailDTO> properties = propertyService.getOwnerProperties(ownerId);

            return ResponseEntity.ok(ApiResponse.success("Lấy danh sách tài sản thành công", properties));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.error("Lỗi server: " + e.getMessage()));
        }
    }

}