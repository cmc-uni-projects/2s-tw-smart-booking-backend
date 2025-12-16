// src/main/java/com/example/smart_booking_system.exception/TwoFactorRequiredException.java

package com.example.smart_booking_system.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

// Tùy chọn: Dùng @ResponseStatus nếu GlobalExceptionHandler chưa bắt lỗi này
// @ResponseStatus(HttpStatus.UNAUTHORIZED)
@Getter // ✅ THÊM LOMBOK @Getter HOẶC VIẾT GETTER THỦ CÔNG
public class TwoFactorRequiredException extends RuntimeException {

    // ✅ THÊM TRƯỜNG MỚI ĐỂ LƯU TOKEN
    private final String twoFactorSessionToken;

    public TwoFactorRequiredException(String message, String twoFactorSessionToken) {
        super(message);
        this.twoFactorSessionToken = twoFactorSessionToken;
    }

    // ✅ PHƯƠNG THỨC THIẾU (Nếu không dùng Lombok @Getter)
    // public String getTwoFactorSessionToken() {
    //     return twoFactorSessionToken;
    // }
}