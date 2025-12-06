package com.example.smart_booking_system.controller;

import com.example.smart_booking_system.dto.PromotionResponseDTO;
import com.example.smart_booking_system.dto.request.promotion.PromotionRequestDTO;
import com.example.smart_booking_system.dto.response.ApiResponse;
import com.example.smart_booking_system.service.PromotionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/promotions")
@RequiredArgsConstructor
public class PromotionController {

    private final PromotionService promotionService;

    // 1. TẠO MỚI (Admin hoặc Owner)
    @PostMapping("/create")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')") // ✅ SỬA: Thêm quyền OWNER
    public ResponseEntity<?> create(@Valid @RequestBody PromotionRequestDTO req) {
        try {
            return ResponseEntity.status(201).body(
                    ApiResponse.success("Tạo mã khuyến mãi thành công", promotionService.createPromotion(req))
            );
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // 2. CẬP NHẬT
    @PutMapping("/update/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')") // ✅ SỬA: Thêm quyền OWNER
    public ResponseEntity<?> update(@PathVariable int id, @Valid @RequestBody PromotionRequestDTO req) {
        try {
            return ResponseEntity.ok(
                    ApiResponse.success("Cập nhật thành công", promotionService.updatePromotion(id, req))
            );
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // 3. XÓA MỀM
    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')") // ✅ SỬA: Thêm quyền OWNER
    public ResponseEntity<?> delete(@PathVariable int id) {
        try {
            promotionService.deletePromotion(id);
            return ResponseEntity.ok(ApiResponse.success("Đã xóa mã khuyến mãi (Chuyển sang thùng rác)"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // 4. BẬT/TẮT TRẠNG THÁI
    @PutMapping("/toggle/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')") // ✅ SỬA: Thêm quyền OWNER
    public ResponseEntity<?> toggle(@PathVariable int id) {
        try {
            return ResponseEntity.ok(
                    ApiResponse.success("Thay đổi trạng thái thành công", promotionService.toggleStatus(id))
            );
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // 5. LẤY TẤT CẢ (TOÀN SÀN - Dành cho ADMIN quản lý hoặc User xem list chung)
    @GetMapping("/all")
    public ResponseEntity<?> getAllGlobalPromotions() {
        return ResponseEntity.ok(
                ApiResponse.success(promotionService.getAllGlobalPromotions())
        );
    }

    // 6. ✅ API MỚI: LẤY DANH SÁCH MÃ CỦA PROPERTY CỤ THỂ (Dành cho Owner/Admin xem)
    @GetMapping("/property/{propertyId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<?> getPromotionsByProperty(@PathVariable int propertyId) {
        try {
            return ResponseEntity.ok(
                    ApiResponse.success(promotionService.getPromotionsByProperty(propertyId))
            );
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // 7. CHI TIẾT
    @GetMapping("/detail/{id}")
    public ResponseEntity<?> getDetail(@PathVariable int id) {
        try {
            return ResponseEntity.ok(ApiResponse.success(promotionService.getById(id)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // 8. UPLOAD BANNER
    @PostMapping(value = "/{id}/banner", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')") // ✅ SỬA: Thêm quyền OWNER
    public ResponseEntity<?> uploadBanner(
            @PathVariable int id,
            @RequestParam("file") MultipartFile file
    ) {
        try {
            return ResponseEntity.ok(
                    ApiResponse.success(
                            "Cập nhật banner thành công",
                            promotionService.uploadBanner(id, file)
                    )
            );
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // 9. GỢI Ý MÃ GIẢM GIÁ (User dùng khi đặt phòng)
    @GetMapping("/suggest")
    public ResponseEntity<ApiResponse<PromotionResponseDTO>> suggestPromotion(
            @RequestParam String userId,
            @RequestParam(required = false) Integer propertyId, // ✅ SỬA: Thêm tham số propertyId (có thể null)
            @RequestParam BigDecimal amount) {

        // Gọi hàm service với logic mới (3 tham số)
        PromotionResponseDTO bestPromo = promotionService.suggestBestPromotion(userId, propertyId, amount);

        if (bestPromo != null) {
            return ResponseEntity.ok(ApiResponse.success(
                    "Đã tìm thấy mã giảm giá tốt nhất cho bạn.",
                    bestPromo
            ));
        } else {
            return ResponseEntity.ok(ApiResponse.success(
                    "Hiện không có mã giảm giá nào phù hợp.",
                    null
            ));
        }
    }
}