package com.example.smart_booking_system.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
// ✅ THÊM IMPORTS THIẾU
import lombok.Builder;
import lombok.RequiredArgsConstructor; // Cần thiết nếu bạn muốn giữ NoArgsConstructor/AllArgsConstructor

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder // ✅ FIX 1: THÊM @Builder VÀO ĐÂY
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private boolean success;
    private String message;
    private T data;
    private LocalDateTime timestamp;
    private String path;
    // 🛑 THIẾU TRƯỜNG CODE: Thêm trường này nếu bạn dùng nó trong requiredTwoFactor
    private String code;


    // Success response with data
    public static <T> ApiResponse<T> success(T data) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setSuccess(true);
        response.setMessage("Operation successful");
        response.setData(data);
        response.setTimestamp(LocalDateTime.now());
        return response;
    }

    // Success response with custom message
    public static <T> ApiResponse<T> success(String message, T data) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setSuccess(true);
        response.setMessage(message);
        response.setData(data);
        response.setTimestamp(LocalDateTime.now());
        return response;
    }

    // Success response without data
    public static <T> ApiResponse<T> success(String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setSuccess(true);
        response.setMessage(message);
        response.setTimestamp(LocalDateTime.now());
        return response;
    }

    // Error response
    public static <T> ApiResponse<T> error(String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setSuccess(false);
        response.setMessage(message);
        response.setTimestamp(LocalDateTime.now());
        return response;
    }

    // Error response with path
    public static <T> ApiResponse<T> error(String message, String path) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setSuccess(false);
        response.setMessage(message);
        response.setPath(path);
        response.setTimestamp(LocalDateTime.now());
        return response;
    }

    // ✅ FIX 2: PHƯƠNG THỨC requiredTwoFactor
    // Dùng @Builder để tạo instance cho trường hợp đặc biệt này
    public static <T> ApiResponse<T> requiredTwoFactor(String message, T data) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .code("2FA_REQUIRED") // Sử dụng trường code
                .data(data)
                .timestamp(LocalDateTime.now()) // Thêm timestamp
                .build();
    }
}