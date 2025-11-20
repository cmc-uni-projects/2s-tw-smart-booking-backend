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
    public ResponseEntity<List<PropertyDetailDTO>> searchProperties(
                                                                     @RequestParam(required = false) String keyword,
                                                                     @RequestParam(required = false, defaultValue = "1") Integer guests
    ) {
        return ResponseEntity.ok(propertyService.searchProperties(keyword, guests));
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

    @GetMapping("/my-active-properties")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<?> getMyActiveProperties(Authentication authentication) {
        try {
            String ownerId = authentication.getName(); // Hoặc lấy từ CustomUserDetails
            // Note: Nếu authentication.getName() trả về email, hãy dùng logic userDetails.getUserId() như cũ
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

            return ResponseEntity.ok(ApiResponse.success(
                    "Thành công",
                    propertyService.getOwnerActiveProperties(userDetails.getUserId())
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<PropertyDetailDTO> getPropertyDetail(@PathVariable Integer id) {
        return ResponseEntity.ok(propertyService.getPropertyDetailById(id));
    }

    @GetMapping("/nearby")
    public ResponseEntity<List<PropertyDetailDTO>> findNearby(
            @RequestParam Double lat,
            @RequestParam Double lng,
            @RequestParam(required = false, defaultValue = "10") Double radius
    ) {
        return ResponseEntity.ok(propertyService.findNearbyProperties(lat, lng, radius));
    }

}