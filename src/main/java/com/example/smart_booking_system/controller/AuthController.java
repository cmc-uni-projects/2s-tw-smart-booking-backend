package com.example.smart_booking_system.controller;

import com.example.smart_booking_system.dto.request.auth.*;
import com.example.smart_booking_system.dto.response.ApiResponse;
import com.example.smart_booking_system.dto.response.auth.LoginResponse;
import com.example.smart_booking_system.exception.BadRequestException;
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

import java.util.Date;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentication APIs")
public class AuthController {

    private final AuthService authService;
    private final TokenBlacklistService tokenBlacklistService;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping("/register")
    @Operation(summary = "Register new user", description = "Register a new user account")
    public ResponseEntity<ApiResponse<Void>> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Registration successful. Please check your email to verify your account."));
    }

    @PostMapping("/login")
    @Operation(summary = "Login", description = "Authenticate user and return JWT token")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout", description = "Invalidate current JWT token")
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
    @Operation(summary = "Resend verification email", description = "Resend email verification link")
    public ResponseEntity<ApiResponse<Void>> resendVerification(@RequestParam String email) {
        authService.resendVerificationEmail(email);
        return ResponseEntity.ok(ApiResponse.success("Verification email sent. Please check your inbox."));
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Forgot password", description = "Request password reset link")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok(ApiResponse.success("Password reset link sent to your email."));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset password", description = "Reset password with token")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.success("Password reset successful. You can now login with your new password."));
    }

    @PostMapping("/change-password")
    @Operation(summary = "Change password", description = "Change password for authenticated user")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        authService.changePassword(request, currentUser.getUserId());
        return ResponseEntity.ok(ApiResponse.success("Password changed successfully"));
    }

    @GetMapping("/me")
    @Operation(summary = "Get current user", description = "Get current authenticated user information")
    public ResponseEntity<ApiResponse<CustomUserDetails>> getCurrentUser(
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        return ResponseEntity.ok(ApiResponse.success(currentUser));
    }
}