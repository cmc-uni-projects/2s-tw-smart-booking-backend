package com.example.smart_booking_system.dto.response.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO chứa thông tin cần thiết khi Backend yêu cầu xác thực 2 yếu tố (2FA).
 * Được trả về cùng với code "2FA_REQUIRED" qua ApiResponse.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TwoFactorRequiredResponse {

    /** Session Token ngắn hạn để xác minh OTP */
    private String twoFactorSessionToken;

    /** Email của người dùng (dùng để hiển thị trong modal OTP) */
    private String email;

    // Có thể thêm userId nếu cần, nhưng Session Token đã chứa thông tin này
}