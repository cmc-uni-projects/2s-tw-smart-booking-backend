package com.example.smart_booking_system.controller;

import com.example.smart_booking_system.dto.request.user.UserDetailRequestDTO;
import com.example.smart_booking_system.dto.response.ApiResponse;
import com.example.smart_booking_system.dto.response.user.UserDetailResponseDTO;
import com.example.smart_booking_system.exception.BadRequestException;
import com.example.smart_booking_system.exception.ConflictException;
import com.example.smart_booking_system.exception.ResourceNotFoundException;
import com.example.smart_booking_system.security.CustomUserDetails;
import com.example.smart_booking_system.service.UserDetailService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/userdetails")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CUSTOMER') or hasRole('OWNER') or hasRole('ADMIN')")
public class UserDetailController {

    private final UserDetailService userDetailService;

    // ==========================================================
    // 1. THÊM MỚI (ADD) - User tự thêm chi tiết cho mình
    // ==========================================================
    @PostMapping("/add")
    public ResponseEntity<ApiResponse<?>> createUserDetail(
            @Valid @RequestBody UserDetailRequestDTO dto,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        try {
            UserDetailResponseDTO newDetail = userDetailService.createUserDetail(dto, currentUser.getUserId());
            return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Tạo chi tiết người dùng thành công", newDetail));
        } catch (ConflictException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error("Lỗi khi tạo chi tiết người dùng: " + e.getMessage()));
        }
    }

    // ==========================================================
    // 2. TÌM KIẾM (SEARCH) - User tự lấy chi tiết của mình
    // ==========================================================
    @GetMapping("/search")
    public ResponseEntity<?> getMyUserDetail(
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        try {
            UserDetailResponseDTO detail = userDetailService.getUserDetailByUserId(currentUser.getUserId());
            return ResponseEntity.ok(ApiResponse.success(detail));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error("Lỗi khi lấy chi tiết người dùng: " + e.getMessage()));
        }
    }

    // ==========================================================
    // 3. CẬP NHẬT (EDIT) - User tự cập nhật chi tiết của mình
    // ==========================================================
    @PutMapping("/edit")
    public ResponseEntity<ApiResponse<?>> updateUserDetail(
            @Valid @RequestBody UserDetailRequestDTO dto,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        try {
            UserDetailResponseDTO updatedDetail = userDetailService.updateUserDetail(dto, currentUser.getUserId());
            return ResponseEntity.ok(ApiResponse.success("Cập nhật chi tiết người dùng thành công", updatedDetail));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error("Lỗi khi cập nhật chi tiết người dùng: " + e.getMessage()));
        }
    }

    // ==========================================================
    // 4. XÓA MỀM (DELETE) - User tự xóa chi tiết của mình
    // ==========================================================
    @DeleteMapping("/delete")
    public ResponseEntity<ApiResponse<?>> deleteUserDetail(
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        try {
            String message = userDetailService.deleteUserDetail(currentUser.getUserId());
            return ResponseEntity.ok(ApiResponse.success(message));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error("Lỗi khi xóa chi tiết người dùng: " + e.getMessage()));
        }
    }

    // ==========================================================
    // HÀM PHỤ: User tự UPLOAD ẢNH
    // ==========================================================
    @PostMapping("/upload-photo")
    public ResponseEntity<ApiResponse<?>> uploadPhoto(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @RequestParam("image") MultipartFile imageFile) { // <-- Nhận file với key là "image"

        try {
            UserDetailResponseDTO updatedDetail = userDetailService.uploadProfilePhoto(
                    currentUser.getUserId(),
                    imageFile
            );
            return ResponseEntity.ok(ApiResponse.success("Upload ảnh đại diện thành công", updatedDetail));
        } catch (BadRequestException e) {
            // Lỗi do người dùng (file rỗng, file không phải ảnh)
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            // Lỗi server (không lưu được file,...)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error("Lỗi khi upload ảnh: " + e.getMessage()));
        }
    }
}