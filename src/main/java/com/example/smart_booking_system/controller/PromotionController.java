package com.example.smart_booking_system.controller;

import com.example.smart_booking_system.dto.PromotionResponseDTO;
import com.example.smart_booking_system.dto.request.promotion.PromotionRequestDTO;
import com.example.smart_booking_system.dto.response.ApiResponse;
import com.example.smart_booking_system.service.PromotionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/promotions")
@RequiredArgsConstructor
public class PromotionController {

    private final PromotionService promotionService;

    @PostMapping("/create")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> create(@Valid @RequestBody PromotionRequestDTO req) {
        try {
            return ResponseEntity.status(201).body(
                    ApiResponse.success("Tạo mã toàn sàn thành công", promotionService.createPromotion(req))
            );
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/update/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> update(@PathVariable int id, @Valid @RequestBody PromotionRequestDTO req) {
        try {
            return ResponseEntity.ok(
                    ApiResponse.success("Cập nhật thành công", promotionService.updatePromotion(id, req))
            );
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // DELETE: Xóa mềm (Chuyển sang DELETED)
    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> delete(@PathVariable int id) {
        try {
            promotionService.deletePromotion(id);
            return ResponseEntity.ok(ApiResponse.success("Đã xóa mã khuyến mãi (Chuyển sang thùng rác)"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // TOGGLE: Bật/Tắt (ACTIVE <-> PAUSED)
    @PutMapping("/toggle/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> toggle(@PathVariable int id) {
        try {
            return ResponseEntity.ok(
                    ApiResponse.success("Thay đổi trạng thái Bật/Tắt thành công", promotionService.toggleStatus(id))
            );
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/all")
    public ResponseEntity<?> getAllPromotions() {
        return ResponseEntity.ok(
                ApiResponse.success(promotionService.getAllGlobalPromotions())
        );
    }

    @GetMapping("/detail/{id}")
    public ResponseEntity<?> getDetail(@PathVariable int id) {
        try {
            return ResponseEntity.ok(ApiResponse.success(promotionService.getById(id)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    //  Upload Banner
    @PostMapping(value = "/{id}/banner", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
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
    @GetMapping("/suggest")
    public ResponseEntity<ApiResponse<PromotionResponseDTO>> suggestPromotion(
            @RequestParam String userId,
            @RequestParam BigDecimal amount) {

        PromotionResponseDTO bestPromo = promotionService.suggestBestPromotion(userId, amount);

        if (bestPromo != null) {
            // ✅ SỬ DỤNG ApiResponse.success(message, data)
            return ResponseEntity.ok(ApiResponse.success(
                    "Đã tìm thấy mã giảm giá tốt nhất cho bạn.",
                    bestPromo
            ));
        } else {
            // ✅ SỬ DỤNG ApiResponse.success(message, data) với data = null
            return ResponseEntity.ok(ApiResponse.success(
                    "Hiện không có mã giảm giá nào phù hợp.",
                    null
            ));
        }
    }
}