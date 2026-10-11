package com.example.smart_booking_system.controller;

import com.example.smart_booking_system.i18n.Messages;
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
public class AuthController {

    private final Messages messages;

    private final AuthService authService;
    private final TokenBlacklistService tokenBlacklistService;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Void>> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(messages.t("api.auth.registered")));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success(messages.t("api.auth.loggedIn"), response));
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

        return ResponseEntity.ok(ApiResponse.success(messages.t("api.auth.loggedOut")));
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
                    "message", messages.of(e)
            ));
        }
    }



    @PostMapping("/resend-verification")
    public ResponseEntity<ApiResponse<Void>> resendVerification(@RequestParam String email) {
        authService.resendVerificationEmail(email);
        return ResponseEntity.ok(ApiResponse.success(messages.t("api.auth.verificationSent")));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok(ApiResponse.success(messages.t("api.auth.resetLinkSent")));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.success(messages.t("api.auth.passwordReset")));
    }

    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        authService.changePassword(request, currentUser.getUserId());
        return ResponseEntity.ok(ApiResponse.success(messages.t("api.auth.passwordChanged")));
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

        return ResponseEntity.ok(ApiResponse.success(messages.t("api.social.unlinked", provider)));
    }
    @PostMapping("/create-password")
    public ResponseEntity<ApiResponse<Void>> createPassword(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @RequestBody Map<String, String> request) { // Nhận JSON { "password": "..." }

        String newPassword = request.get("password");
        if (newPassword == null || newPassword.length() < 6) {
            throw new BadRequestException("error.password.weak");
        }

        authService.createPassword(currentUser.getUserId(), newPassword);
        return ResponseEntity.ok(ApiResponse.success(messages.t("api.password.created")));
    }
}