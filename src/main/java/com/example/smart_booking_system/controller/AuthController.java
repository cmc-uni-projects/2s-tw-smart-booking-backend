package com.example.smart_booking_system.controller;

import com.example.smart_booking_system.dto.request.auth.*;
import com.example.smart_booking_system.dto.response.ApiResponse;
import com.example.smart_booking_system.dto.response.auth.LoginResponse;
import com.example.smart_booking_system.entity.User;
import com.example.smart_booking_system.exception.BadRequestException;
import com.example.smart_booking_system.exception.UnauthorizedException;
// ✅ THÊM IMPORTS CẦN THIẾT CHO 2FA
import com.example.smart_booking_system.exception.TwoFactorRequiredException;
import com.example.smart_booking_system.dto.response.auth.TwoFactorRequiredResponse;

import com.example.smart_booking_system.security.CustomUserDetails;
import com.example.smart_booking_system.security.JwtTokenProvider;
import com.example.smart_booking_system.service.AuthService;
import com.example.smart_booking_system.service.TokenBlacklistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import com.example.smart_booking_system.service.TwoFactorService;
import com.example.smart_booking_system.dto.request.auth.TwoFactorVerifyRequest;
import java.util.Date;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final TokenBlacklistService tokenBlacklistService;
    private final JwtTokenProvider jwtTokenProvider;
    private final TwoFactorService twoFactorService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Void>> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Registration successful. Please check your email to verify your account."));
    }

    // 🛑 PHƯƠNG THỨC LOGIN ĐÃ FIX LỖI 2FA
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            // 1. Nếu 2FA TẮT: Trả về JWT
            LoginResponse response = authService.login(request);
            return ResponseEntity.ok(ApiResponse.success("Login successful", response));

        } catch (TwoFactorRequiredException e) {
            // 2. ✅ FIX: Nếu 2FA BẬT: Bắt Exception và trả về Session Token.

            String sessionToken = e.getTwoFactorSessionToken();

            // Xử lý nếu Token bị null (lỗi trong AuthService)
            if (sessionToken == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(ApiResponse.error("2FA Error: Missing session token."));
            }

            // Xây dựng Response body mong muốn của Frontend
            TwoFactorRequiredResponse response = TwoFactorRequiredResponse.builder()
                    .twoFactorSessionToken(sessionToken)
                    .email(request.getEmail())
                    .build();

            // Trả về 401 Unauthorized để Frontend (Login.jsx) catch lỗi và hiển thị modal
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.requiredTwoFactor("2FA required.", response));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            Date exp = jwtTokenProvider.extractExpiration(token);
            tokenBlacklistService.blacklist(token, exp.toInstant()
                    .atZone(java.time.ZoneId.systemDefault())
                    .toLocalDateTime());
        }

        return ResponseEntity.ok(ApiResponse.success("Logout successful"));
    }

    @GetMapping("/verify-email")
    public ResponseEntity<Map<String, Object>> verifyEmail(@RequestParam("token") String token) {
        try {
            authService.verifyEmail(token);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Email verified successfully"
            ));
        } catch (BadRequestException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }


    @PostMapping("/resend-verification")
    public ResponseEntity<ApiResponse<Void>> resendVerification(@RequestParam String email) {
        authService.resendVerificationEmail(email);
        return ResponseEntity.ok(ApiResponse.success("Verification email sent. Please check your inbox."));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok(ApiResponse.success("Password reset link sent to your email."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.success("Password reset successful. You can now login with your new password."));
    }

    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        authService.changePassword(request, currentUser.getUserId());
        return ResponseEntity.ok(ApiResponse.success("Password changed successfully"));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<CustomUserDetails>> getCurrentUser(
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        return ResponseEntity.ok(ApiResponse.success(currentUser));
    }

    @DeleteMapping("/social/unlink")
    public ResponseEntity<ApiResponse<Void>> unlinkSocialAccount(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @RequestParam String provider) {

        authService.unlinkSocialAccount(currentUser.getUserId(), provider);

        return ResponseEntity.ok(ApiResponse.success("Đã ngắt kết nối tài khoản " + provider));
    }

    @PostMapping("/create-password")
    public ResponseEntity<ApiResponse<Void>> createPassword(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @RequestBody Map<String, String> request) { // Nhận JSON { "password": "..." }

        String newPassword = request.get("password");
        if (newPassword == null || newPassword.length() < 6) {
            throw new BadRequestException("Mật khẩu phải có ít nhất 6 ký tự.");
        }

        authService.createPassword(currentUser.getUserId(), newPassword);
        return ResponseEntity.ok(ApiResponse.success("Tạo mật khẩu thành công"));
    }

    // AuthController.java - BÊN TRONG verifyTwoFactor
    @PostMapping("/verify-2fa")
    public ResponseEntity<ApiResponse<LoginResponse>> verifyTwoFactor(
            @Valid @RequestBody TwoFactorVerifyRequest request
    ) {
        // 1. Validate và lấy User từ Session Token
        User user = twoFactorService.validateSessionToken(request.getTwoFactorSessionToken());

        // 2. Xác thực OTP
        if (twoFactorService.validateOtp(user, request.getOtpCode())) {

            // 3. KHÔI PHỤC LOGIC TẠO ROLES VÀ USER RESPONSE

            // 3a. Tạo roles
            Set<String> roles = user.getRoles().stream()
                    .map(role -> role.getRoleName())
                    .collect(java.util.stream.Collectors.toSet());

            // 3b. Lấy thông tin User Response
            LoginResponse.UserResponse userResponse = new LoginResponse.UserResponse(
                    user.getUserId(),
                    user.getFullName(),
                    user.getEmail(),
                    user.getPhoneNumber(),
                    user.getIsEmailVerified(),
                    user.getStatus(),
                    roles
            );

            // 4. Tạo JWT (Full Access)
            String jwt = jwtTokenProvider.generateToken(user.getUserId(), user.getEmail(), roles);
            long expiresIn = jwtTokenProvider.getExpirationTime();

            // Xóa OTP khỏi DB
            twoFactorService.cleanUpOtp(user);

            // 5. Trả về ApiResponse với LoginResponse đầy đủ
            LoginResponse response = new LoginResponse(jwt, expiresIn, userResponse);

            return ResponseEntity.ok(ApiResponse.success("Login successful with 2FA", response));
        } else {
            // Nếu validateOtp không ném exception, ném UnauthorizedException.
            throw new UnauthorizedException("Invalid OTP Code.");
        }
    }
}