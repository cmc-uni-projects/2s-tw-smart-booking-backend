package com.example.smart_booking_system.controller;

import com.example.smart_booking_system.dto.request.user.UserDetailRequestDTO;
import com.example.smart_booking_system.dto.response.ApiResponse;
import com.example.smart_booking_system.dto.response.user.UserDetailResponseDTO;
import com.example.smart_booking_system.security.CustomUserDetails;
import com.example.smart_booking_system.service.UserDetailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/user-details")
public class UserDetailController {

    private final UserDetailService userDetailService;

    @Autowired
    public UserDetailController(UserDetailService userDetailService) {
        this.userDetailService = userDetailService;
    }

    /**
     * Lấy thông tin hồ sơ. Trả về DTO trực tiếp (để sửa Lỗi 1 của ProfilePage)
     */
    @GetMapping("/me")
    public ResponseEntity<UserDetailResponseDTO> getUserDetail(@AuthenticationPrincipal CustomUserDetails currentUser) {
        UserDetailResponseDTO userDetail = userDetailService.getUserDetail(currentUser.getEmail());
        return ResponseEntity.ok(userDetail);
    }

    /**
     * Cập nhật thông tin hồ sơ.
     * Trả về ApiResponse chứa DTO đã cập nhật (để sửa Lỗi 3 của ProfilePage)
     */
    @PutMapping("/update")
    public ResponseEntity<ApiResponse<UserDetailResponseDTO>> updateUserDetail(@AuthenticationPrincipal CustomUserDetails currentUser, @RequestBody UserDetailRequestDTO userDetailRequestDTO) {

        // Service giờ sẽ trả về DTO đã cập nhật
        UserDetailResponseDTO updatedProfile = userDetailService.updateUserDetail(currentUser.getEmail(), userDetailRequestDTO);

        // Trả về DTO trong data (Sửa lỗi ApiResponse constructor và Lỗi 3)
        return ResponseEntity.ok(ApiResponse.success("Profile updated successfully", updatedProfile));
    }

    /**
     * (Giai đoạn 2) Endpoint kiểm tra trạng thái hồ sơ.
     * Trả về DTO { isProfileComplete: true/false } trực tiếp.
     */
    @GetMapping("/profile-status")
    public ResponseEntity<?> getProfileStatus(@AuthenticationPrincipal CustomUserDetails currentUser) {
        if (currentUser == null) {
            // Sửa lỗi ApiResponse constructor
            return ResponseEntity.status(401).body(ApiResponse.error("User not authenticated"));
        }

        UserDetailService.ProfileStatusResponse response = userDetailService.checkProfileCompleteness(currentUser.getEmail());
        return ResponseEntity.ok(response); // Trả về DTO { isProfileComplete: ... }
    }
}