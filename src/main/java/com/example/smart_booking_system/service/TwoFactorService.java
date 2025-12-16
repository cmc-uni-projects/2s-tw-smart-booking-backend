// src/main/java/com/example/smart_booking_system/service/TwoFactorService.java

package com.example.smart_booking_system.service;

import com.example.smart_booking_system.entity.User;
import com.example.smart_booking_system.dto.request.auth.TwoFactorVerifyRequest;
import com.example.smart_booking_system.dto.request.auth.LoginRequest; // Cần dùng cho logic Bật 2FA

public interface TwoFactorService {

    String generateAndSendOtp(User user);

    User validateSessionToken(String token);

    boolean validateOtp(User user, String otpCode);

    void cleanUpOtp(User user);

    void enable2FA(User user);

    void disable2FA(User user, LoginRequest credentials); // Yêu cầu xác thực lại mật khẩu

    // Lấy trạng thái 2FA
    boolean is2FAEnabled(User user);
}