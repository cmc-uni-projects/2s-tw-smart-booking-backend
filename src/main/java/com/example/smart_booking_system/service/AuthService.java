package com.example.smart_booking_system.service;

import com.example.smart_booking_system.dto.request.auth.*;
import com.example.smart_booking_system.dto.response.auth.LoginResponse;

public interface AuthService {

    void register(RegisterRequest request);

    LoginResponse login(LoginRequest request);

    void verifyEmail(String token);

    void forgotPassword(ForgotPasswordRequest request);

    void resetPassword(ResetPasswordRequest request);

    void changePassword(ChangePasswordRequest request, String userId);

    void resendVerificationEmail(String email);
}
