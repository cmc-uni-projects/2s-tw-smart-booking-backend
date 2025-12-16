// src/main/java/com/example/smart_booking_system/controller/TwoFactorController.java

package com.example.smart_booking_system.controller;

import com.example.smart_booking_system.dto.response.auth.LoginResponse;
import com.example.smart_booking_system.service.TwoFactorService;
import com.example.smart_booking_system.service.UserDetailService;
import com.example.smart_booking_system.dto.request.auth.TwoFactorVerifyRequest;
import com.example.smart_booking_system.dto.request.auth.LoginRequest;
import com.example.smart_booking_system.dto.response.ApiResponse;
import com.example.smart_booking_system.entity.User;
import com.example.smart_booking_system.security.JwtTokenProvider;
import com.example.smart_booking_system.exception.ConflictException;
import com.example.smart_booking_system.exception.UnauthorizedException;
import com.example.smart_booking_system.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/user/security/2fa")
@RequiredArgsConstructor
public class TwoFactorController {

    private final TwoFactorService twoFactorService;
    private final UserDetailService userDetailService; // FIX: Đổi tên dependency

    // Endpoint 1: LẤY TRẠNG THÁI HIỆN TẠI
    @GetMapping("/status")
    public ApiResponse<Boolean> get2FAStatus(@AuthenticationPrincipal CustomUserDetails userDetails) {
        // FIX: Gọi service mới
        User user = userDetailService.findById(userDetails.getUserId());
        // FIX: Sửa thứ tự tham số: message trước, data (Boolean) sau
        return ApiResponse.success("2FA status retrieved successfully.", user.isUsing2FA());
    }

    // Endpoint 2: YÊU CẦU BẬT 2FA (Gửi OTP)
    @PostMapping("/enable/request")
    public ApiResponse<String> requestEnable2FA(@AuthenticationPrincipal CustomUserDetails userDetails) {
        // FIX: Gọi service mới
        User user = userDetailService.findById(userDetails.getUserId());
        if (user.isUsing2FA()) {
            throw new ConflictException("2FA is already enabled.");
        }
        twoFactorService.generateAndSendOtp(user);
        return ApiResponse.success("OTP sent to your email. Please verify to enable 2FA.");
    }

    // Endpoint 3: XÁC NHẬN OTP ĐỂ BẬT 2FA
    @PostMapping("/enable/verify")
    public ApiResponse<LoginResponse> verifyEnable2FA( // 🛑 SỬA: Thay Void bằng LoginResponse
                                                       @AuthenticationPrincipal CustomUserDetails userDetails,
                                                       @RequestBody TwoFactorVerifyRequest request
    ) {
        User user = userDetailService.findById(userDetails.getUserId());

        // 1. Xác thực OTP
        twoFactorService.validateOtp(user, request.getOtpCode());

        // 2. Bật 2FA (Đã cập nhật DB và Context)
        twoFactorService.enable2FA(user);

        // 3. ✅ FIX LỖI CACHE: TẠO JWT MỚI VÀ TRẢ VỀ CHO FRONTEND

        // Đảm bảo lấy lại user entity đã được cập nhật từ DB (để tạo token mới)
        User updatedUser = userDetailService.findById(userDetails.getUserId());

        Set<String> roles = updatedUser.getRoles().stream()
                .map(role -> role.getRoleName())
                .collect(Collectors.toSet());

        LoginResponse.UserResponse userResponse = new LoginResponse.UserResponse(
                updatedUser.getUserId(),
                updatedUser.getEmail(),
                updatedUser.getFullName(),
                updatedUser.getPhoneNumber(),
                updatedUser.getIsEmailVerified(),
                updatedUser.getStatus(),
                roles
        );

        String jwt = twoFactorService.getJwtTokenProvider().generateToken(updatedUser.getUserId(), updatedUser.getEmail(), roles);
        long expiresIn = twoFactorService.getJwtTokenProvider().getExpirationTime();

        LoginResponse response = new LoginResponse(jwt, expiresIn, userResponse);

        // 4. Trả về Token mới (chứa isUsing2FA=true)
        return ApiResponse.success("2FA enabled successfully. Please use new token.", response);
    }

    // Endpoint 4: TẮT 2FA (Yêu cầu nhập lại mật khẩu)
    @PostMapping("/disable")
    public ApiResponse<Void> disable2FA(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody LoginRequest credentials
    ) {
        // FIX: Gọi service mới
        User user = userDetailService.findById(userDetails.getUserId());
        twoFactorService.disable2FA(user, credentials);
        return ApiResponse.success("2FA disabled successfully. Password matched.");
    }
}