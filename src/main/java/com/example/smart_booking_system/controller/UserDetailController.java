package com.example.smart_booking_system.controller;

import com.example.smart_booking_system.i18n.Messages;
import com.example.smart_booking_system.dto.request.user.UserDetailRequestDTO;
import com.example.smart_booking_system.dto.response.ApiResponse;
import com.example.smart_booking_system.dto.response.user.UserDetailResponseDTO;
import com.example.smart_booking_system.security.CustomUserDetails;
import com.example.smart_booking_system.service.UserDetailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.HttpStatus;
import com.example.smart_booking_system.exception.BadRequestException;

@RestController
@RequestMapping("/api/v1/user-details")
public class UserDetailController {

    @Autowired
    private Messages messages;

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
        return ResponseEntity.ok(ApiResponse.success(messages.t("api.profile.updated"), updatedProfile));
    }

    /** Body {"language": "vi" | "en"}: language of this user's emails and notifications. */
    @PutMapping("/language")
    public ResponseEntity<ApiResponse<java.util.Map<String, String>>> updateLanguage(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @RequestBody java.util.Map<String, String> body) {
        String language = userDetailService.updatePreferredLanguage(currentUser.getUserId(), body.get("language"));
        return ResponseEntity.ok(ApiResponse.success(messages.t("api.language.updated"), java.util.Map.of("language", language)));
    }

    /**
     * (Giai đoạn 2) Endpoint kiểm tra trạng thái hồ sơ.
     * Trả về DTO { isProfileComplete: true/false } trực tiếp.
     */
    @GetMapping("/profile-status")
    public ResponseEntity<?> getProfileStatus(@AuthenticationPrincipal CustomUserDetails currentUser) {
        if (currentUser == null) {
            // Sửa lỗi ApiResponse constructor
            return ResponseEntity.status(401).body(ApiResponse.error(messages.t("error.notAuthenticated")));
        }

        UserDetailService.ProfileStatusResponse response = userDetailService.checkProfileCompleteness(currentUser.getEmail());
        return ResponseEntity.ok(response); // Trả về DTO { isProfileComplete: ... }
    }
    @PostMapping("/upload-avatar")
    public ResponseEntity<ApiResponse<?>> uploadPhoto(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            // ✅ SỬA 3: Đổi tên tham số từ "image" thành "file" (vì frontend gửi formData.append("file", ...))
            @RequestParam("file") MultipartFile imageFile) {

        try {
            UserDetailResponseDTO updatedDetail = userDetailService.uploadProfilePhoto(
                    currentUser.getUserId(),
                    imageFile
            );
            return ResponseEntity.ok(ApiResponse.success(messages.t("api.avatar.updated"), updatedDetail));
        } catch (BadRequestException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(messages.of(e)));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error(messages.of(e)));
        }
    }
}