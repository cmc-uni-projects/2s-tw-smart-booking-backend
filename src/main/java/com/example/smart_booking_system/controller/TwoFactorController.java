// src/main/java/com/example/smart_booking_system/controller/TwoFactorController.java

package com.example.smart_booking_system.controller;

import com.example.smart_booking_system.service.TwoFactorService;
import com.example.smart_booking_system.service.UserDetailService; // FIX: Đổi từ UserService (không tìm thấy) sang UserDetailService (có trong project)
import com.example.smart_booking_system.dto.request.auth.TwoFactorVerifyRequest;
import com.example.smart_booking_system.dto.request.auth.LoginRequest;
import com.example.smart_booking_system.dto.response.ApiResponse;
import com.example.smart_booking_system.entity.User;
import com.example.smart_booking_system.exception.ConflictException;
import com.example.smart_booking_system.exception.UnauthorizedException;
import com.example.smart_booking_system.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user/security/2fa")
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
    public ApiResponse<Void> verifyEnable2FA(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody TwoFactorVerifyRequest request
    ) {
        // FIX: Gọi service mới
        User user = userDetailService.findById(userDetails.getUserId());

        // 1. Xác thực OTP. Hàm này sẽ ném exception nếu OTP sai/hết hạn.
        twoFactorService.validateOtp(user, request.getOtpCode());

        // 2. Nếu không có exception, bật 2FA.
        twoFactorService.enable2FA(user);

        // 3. Trả về thành công
        return ApiResponse.success("2FA enabled successfully.");
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