// src/main/java/com/example/smart_booking_system/controller/TwoFactorController.java

package com.example.smart_booking_system.controller;

// ... Imports ...
import com.example.smart_booking_system.service.TwoFactorService;
import com.example.smart_booking_system.dto.request.auth.TwoFactorVerifyRequest;
import com.example.smart_booking_system.dto.request.auth.LoginRequest;
import com.example.smart_booking_system.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.http.ResponseEntity;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user/security/2fa") // Path mới, tách biệt khỏi user/details
@RequiredArgsConstructor
public class TwoFactorController {

    private final TwoFactorService twoFactorService;
    private final UserService userService; // Giả định có UserService để lấy User Entity

    // Endpoint 1: LẤY TRẠNG THÁI HIỆN TẠI
    @GetMapping("/status")
    public ApiResponse<Boolean> get2FAStatus(@AuthenticationPrincipal CustomUserDetails userDetails) {
        User user = userService.findById(userDetails.getId());
        return new ApiResponse<>(user.isUsing2FA(), "2FA status retrieved successfully.");
    }

    // Endpoint 2: YÊU CẦU BẬT 2FA (Gửi OTP)
    @PostMapping("/enable/request")
    public ApiResponse<String> requestEnable2FA(@AuthenticationPrincipal CustomUserDetails userDetails) {
        User user = userService.findById(userDetails.getId());
        if (user.isUsing2FA()) {
            throw new ConflictException("2FA is already enabled.");
        }
        // Gửi OTP. Session Token không cần thiết ở đây vì người dùng đã login
        twoFactorService.generateAndSendOtp(user);
        return new ApiResponse<>("OTP sent to your email. Please verify to enable 2FA.");
    }

    // Endpoint 3: XÁC NHẬN OTP ĐỂ BẬT 2FA
    @PostMapping("/enable/verify")
    public ApiResponse<Void> verifyEnable2FA(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody TwoFactorVerifyRequest request // Dùng lại DTO, nhưng chỉ cần otpCode
    ) {
        User user = userService.findById(userDetails.getId());

        // Xác thực OTP
        if (twoFactorService.validateOtp(user, request.getOtpCode())) {
            twoFactorService.enable2FA(user); // Cập nhật trạng thái trong DB
            return new ApiResponse<>(null, "2FA enabled successfully.", HttpStatus.OK.value(), "2FA_ENABLED");
        }
        // Nếu OTP không hợp lệ, validateOtp sẽ ném UnauthorizedException
        return null;
    }

    // Endpoint 4: TẮT 2FA (Yêu cầu nhập lại mật khẩu)
    @PostMapping("/disable")
    public ApiResponse<Void> disable2FA(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody LoginRequest credentials // Dùng lại LoginRequest để lấy mật khẩu
    ) {
        User user = userService.findById(userDetails.getId());
        // Kiểm tra mật khẩu và tắt 2FA trong cùng một phương thức service
        twoFactorService.disable2FA(user, credentials);
        return new ApiResponse<>(null, "2FA disabled successfully. Password matched.", HttpStatus.OK.value(), "2FA_DISABLED");
    }
}