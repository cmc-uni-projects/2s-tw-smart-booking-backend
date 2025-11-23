package com.example.smart_booking_system.controller;

import com.example.smart_booking_system.dto.request.promotion.PromotionRequestDTO;
import com.example.smart_booking_system.dto.response.ApiResponse;
import com.example.smart_booking_system.service.PromotionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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
}