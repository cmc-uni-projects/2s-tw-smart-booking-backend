package com.example.smart_booking_system.controller;

import com.example.smart_booking_system.dto.response.property.PropertyMapDTO;
import com.example.smart_booking_system.enums.PropertyType;
import com.example.smart_booking_system.exception.ForbiddenException;
import com.example.smart_booking_system.service.PropertyService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import com.example.smart_booking_system.service.SearchHistoryService;
import com.example.smart_booking_system.entity.Property;
import org.springframework.web.bind.annotation.*;
import com.example.smart_booking_system.dto.response.property.PropertyDetailDTO;
import org.springframework.security.core.Authentication;
import com.example.smart_booking_system.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;

// --- Imports ---
import com.example.smart_booking_system.dto.request.property.PropertyApplicationSubmitDTO;
import com.example.smart_booking_system.dto.response.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;
// --- End Imports ---

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/properties")
public class PropertyController {

    private final PropertyService propertyService;
    private final SearchHistoryService searchHistoryService;

    // ============================================================
    // ADD PROPERTY
    // ============================================================
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

    // ============================================================
    // SUBMIT APPLICATION
    // ============================================================
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

            if (propertyImages == null || propertyImages.size() < 3) {
                return ResponseEntity.badRequest().body(
                        ApiResponse.error("Bạn phải tải lên ít nhất 3 ảnh cho chỗ nghỉ")
                );
            }

            ObjectMapper objectMapper = new ObjectMapper();
            PropertyApplicationSubmitDTO dto =
                    objectMapper.readValue(propertyDataJson, PropertyApplicationSubmitDTO.class);

            PropertyDetailDTO newProperty =
                    propertyService.submitPropertyApplication(dto, propertyImages, ownerId);

            return ResponseEntity.status(HttpStatus.CREATED).body(
                    ApiResponse.success("Nộp đơn đăng ký cơ sở thành công", newProperty)
            );

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));

        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Dữ liệu JSON không hợp lệ"));

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                    ApiResponse.error("Lỗi khi nộp đơn: " + e.getMessage())
            );
        }
    }


    // ============================================================
    // ✅ [UPDATED] SEARCH API (FILTER + PAGINATION)
    // ============================================================
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Page<PropertyDetailDTO>>> searchProperties(
            // 1. Search Text
            @RequestParam(required = false) String keyword,

            // 2. Filter Params (Sidebar)
            @RequestParam(required = false) List<String> cities,        // List thành phố
            @RequestParam(required = false) List<PropertyType> types,   // List loại hình
            @RequestParam(required = false) List<String> amenities,     // List tiện nghi
            @RequestParam(required = false) BigDecimal minRating,       // Sao tối thiểu
            @RequestParam(required = false) BigDecimal minPrice,        // Giá min
            @RequestParam(required = false) BigDecimal maxPrice,        // Giá max

            // 3. Pagination & Sort
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id,desc") String sort, // Format: field,dir (Vd: price,asc)

            // 4. Basic Params (Logic cũ)
            @RequestParam(required = false, defaultValue = "1") Integer guests,
            @RequestParam(required = false) LocalDate checkIn,
            @RequestParam(required = false) LocalDate checkOut
    ) {
        // Xử lý Sort String (vd: "price,asc" -> Sort Object)
        String[] sortParams = sort.split(",");
        String sortField = sortParams[0];
        Sort.Direction sortDir = sortParams.length > 1 && sortParams[1].equalsIgnoreCase("desc")
                ? Sort.Direction.DESC : Sort.Direction.ASC;

        // Lưu ý: Sort "price" cần xử lý đặc biệt vì price nằm ở bảng Room (OneToMany).
        // Tạm thời nếu sort=price thì để unsorted hoặc sort theo ID để tránh lỗi SQL.
        // Bạn có thể mở rộng Service để handle sort theo giá sau.
        Sort sortObj = sortField.equals("price") ? Sort.unsorted() : Sort.by(sortDir, sortField);

        Pageable pageable = PageRequest.of(page, size, sortObj);

        // Gọi Service mới
        Page<PropertyDetailDTO> result = propertyService.searchProperties(
                keyword, cities, types, amenities,
                minRating, minPrice, maxPrice,
                guests, checkIn, checkOut, pageable
        );

        return ResponseEntity.ok(ApiResponse.success("Tìm kiếm thành công", result));
    }

    // ============================================================
    // UPDATE
    // ============================================================
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

    // ============================================================
    // OTHER GET APIs
    // ============================================================
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
    public ResponseEntity<PropertyDetailDTO> getPropertyDetail(
            @PathVariable Integer id,
            @RequestParam(required = false) LocalDate checkIn,
            @RequestParam(required = false) LocalDate checkOut
    ) {
        return ResponseEntity.ok(propertyService.getPropertyDetailById(id, checkIn, checkOut));
    }

    @GetMapping("/nearby")
    public ResponseEntity<List<PropertyMapDTO>> findNearby(
            @RequestParam Double lat,
            @RequestParam Double lng,
            @RequestParam(required = false, defaultValue = "10") Double radius
    ) {
        return ResponseEntity.ok(propertyService.findNearbyProperties(lat, lng, radius));
    }

    @GetMapping("/check-name")
    public ResponseEntity<ApiResponse<Boolean>> checkNameAvailability(@RequestParam String name) {
        boolean isAvailable = propertyService.checkNameAvailability(name);

        if (isAvailable) {
            return ResponseEntity.ok(ApiResponse.success("Tên hợp lệ", true));
        } else {
            return ResponseEntity.status(409).body(ApiResponse.error("Tên chỗ nghỉ đã tồn tại"));
        }
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<?> togglePropertyStatus(@PathVariable Integer id, Authentication authentication) {
        try {
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            String ownerId = userDetails.getUserId();

            boolean newStatus = propertyService.togglePropertyStatus(id, ownerId);

            return ResponseEntity.ok(ApiResponse.success(
                    newStatus ? "Đã bật hoạt động cơ sở lưu trú" : "Đã tạm ngưng cơ sở lưu trú",
                    newStatus
            ));
        } catch (ForbiddenException e) {
            return ResponseEntity.status(403).body(ApiResponse.error(e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.error("Lỗi: " + e.getMessage()));
        }
    }
}