package com.example.smart_booking_system.controller;

import com.example.smart_booking_system.dto.request.user.UserDetailRequestDTO;
import com.example.smart_booking_system.dto.response.ApiResponse;
import com.example.smart_booking_system.dto.response.user.UserDetailResponseDTO;
import com.example.smart_booking_system.exception.BadRequestException;
import com.example.smart_booking_system.exception.ConflictException;
import com.example.smart_booking_system.exception.ResourceNotFoundException;
import com.example.smart_booking_system.service.UserDetailService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/admin/userdetails")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserDetailController {

    private final UserDetailService userDetailService;

    // ==========================================================
    // Admin LẤY (Search) chi tiết của 1 user bất kỳ
    // GET /api/v1/admin/userdetails/search/{userId}
    // ==========================================================
    @GetMapping("/search/{userId}") // <-- SỬA ĐƯỜNG DẪN
    public ResponseEntity<?> getUserDetail(
            @PathVariable String userId) {
        try {
            UserDetailResponseDTO detail = userDetailService.getUserDetailByUserId(userId);
            return ResponseEntity.ok(ApiResponse.success(detail));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error("Lỗi khi lấy chi tiết người dùng: " + e.getMessage()));
        }
    }

    // ==========================================================
    // Admin CẬP NHẬT (Edit) chi tiết của 1 user bất kỳ
    // PUT /api/v1/admin/userdetails/edit/{userId}
    // ==========================================================
    @PutMapping("/edit/{userId}") // <-- SỬA ĐƯỜNG DẪN
    public ResponseEntity<ApiResponse<?>> updateUserDetail(
            @PathVariable String userId,
            @Valid @RequestBody UserDetailRequestDTO dto) {
        try {
            UserDetailResponseDTO updatedDetail = userDetailService.updateUserDetail(dto, userId);
            return ResponseEntity.ok(ApiResponse.success("Cập nhật chi tiết người dùng thành công", updatedDetail));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error("Lỗi khi cập nhật chi tiết người dùng: " + e.getMessage()));
        }
    }

    // ==========================================================
    // Admin XÓA MỀM (Delete) chi tiết của 1 user bất kỳ
    // DELETE /api/v1/admin/userdetails/delete/{userId}
    // ==========================================================
    @DeleteMapping("/delete/{userId}") // <-- SỬA ĐƯỜNG DẪN
    public ResponseEntity<ApiResponse<?>> deleteUserDetail(
            @PathVariable String userId) {
        try {
            String message = userDetailService.deleteUserDetail(userId);
            return ResponseEntity.ok(ApiResponse.success(message));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error("Lỗi khi xóa chi tiết người dùng: " + e.getMessage()));
        }
    }

    // ==========================================================
    // Admin UPLOAD ẢNH ĐẠI DIỆN cho 1 user bất kỳ
    // POST /api/v1/admin/userdetails/upload-photo/{userId}
    // ==========================================================
    @PostMapping("/upload-photo/{userId}") // <-- SỬA ĐƯỜNG DẪN
    public ResponseEntity<ApiResponse<?>> uploadPhoto(
            @PathVariable String userId,
            @RequestParam("image") MultipartFile imageFile) {

        try {
            UserDetailResponseDTO updatedDetail = userDetailService.uploadProfilePhoto(
                    userId,
                    imageFile
            );
            return ResponseEntity.ok(ApiResponse.success("Upload ảnh đại diện thành công", updatedDetail));
        } catch (BadRequestException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error("Lỗi khi upload ảnh: " + e.getMessage()));
        }
    }
}