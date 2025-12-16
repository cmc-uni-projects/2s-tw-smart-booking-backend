package com.example.smart_booking_system.dto.request.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TwoFactorVerifyRequest {

    // 🛑 Trường BẮT BUỘC cho luồng Login 2FA để xác định phiên/người dùng
    //@NotBlank(message = "Session token không được để trống")
    private String twoFactorSessionToken;

    //@NotBlank(message = "Mã OTP không được để trống")
    //@Size(min = 6, max = 6, message = "Mã OTP phải có đúng 6 ký tự")
    private String otpCode;
}