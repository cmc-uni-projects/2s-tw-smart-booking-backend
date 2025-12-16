// src/main/java/com/example/smart_booking_system/exception/TwoFactorRequiredException.java

package com.example.smart_booking_system.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

// Mặc dù sẽ bị GlobalExceptionHandler bắt, nhưng đặt 401/403 để thể hiện cần bước xác thực thêm
@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class TwoFactorRequiredException extends RuntimeException {
    private final String sessionToken;

    public TwoFactorRequiredException(String message, String sessionToken) {
        super(message);
        this.sessionToken = sessionToken;
    }

    public String getSessionToken() {
        return sessionToken;
    }
}