package com.example.smart_booking_system.exception;

import com.example.smart_booking_system.dto.response.ApiResponse;
import com.example.smart_booking_system.dto.response.auth.TwoFactorRequiredResponse; // ✅ Import này
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<?>> handleResourceNotFoundException(
            ResourceNotFoundException ex, WebRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ex.getMessage(), request.getDescription(false)));
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiResponse<?>> handleBadRequestException(
            BadRequestException ex, WebRequest request) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage(), request.getDescription(false)));
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiResponse<?>> handleUnauthorizedException(
            UnauthorizedException ex, WebRequest request) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(ex.getMessage(), request.getDescription(false)));
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ApiResponse<?>> handleForbiddenException(
            ForbiddenException ex, WebRequest request) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(ex.getMessage(), request.getDescription(false)));
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiResponse<?>> handleConflictException(
            ConflictException ex, WebRequest request) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(ex.getMessage(), request.getDescription(false)));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<?>> handleAccessDeniedException(
            AccessDeniedException ex, WebRequest request) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("Access denied: " + ex.getMessage(), request.getDescription(false)));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationExceptions(
            MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();

        // Vòng lặp qua các lỗi để lấy tên trường và thông báo
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = "Global Error"; // Fallback
            if (error instanceof FieldError) {
                fieldName = ((FieldError) error).getField();
            }
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        ApiResponse<Map<String, String>> response = new ApiResponse<>();
        response.setSuccess(false);
        response.setMessage("Có lỗi xảy ra trong quá trình xác thực dữ liệu.");
        response.setData(errors);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<?>> handleGlobalException(
            Exception ex, WebRequest request) {
        ex.printStackTrace();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Internal server error: " + ex.getMessage(), request.getDescription(false)));
    }

    // 1. Xử lý khi sai Tài khoản hoặc Mật khẩu
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<?>> handleBadCredentialsException(
            BadCredentialsException ex, WebRequest request) {
        // Trả về 401 Unauthorized
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("Tài khoản hoặc mật khẩu không chính xác", request.getDescription(false)));
    }

    // 2. Xử lý khi Tài khoản chưa kích hoạt (Chưa xác thực email)
    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ApiResponse<?>> handleDisabledException(
            DisabledException ex, WebRequest request) {
        // Trả về 403 Forbidden
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("Tài khoản chưa được xác thực. Vui lòng kiểm tra email để kích hoạt!", request.getDescription(false)));
    }

    // 3. (Tùy chọn) Xử lý khi tài khoản bị khóa
    @ExceptionHandler(LockedException.class)
    public ResponseEntity<ApiResponse<?>> handleLockedException(
            LockedException ex, WebRequest request) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("Tài khoản đã bị khóa. Vui lòng liên hệ admin.", request.getDescription(false)));
    }

    // NEW HANDLER for 2FA - ĐÃ SỬA LỖI GETTER VÀ TRẢ VỀ RESPONSE ENTITY
    @ExceptionHandler(TwoFactorRequiredException.class)
    public ResponseEntity<ApiResponse<?>> handleTwoFactorRequiredException(TwoFactorRequiredException ex) {

        // Cấu trúc response data (Payload)
        TwoFactorRequiredResponse responseData = TwoFactorRequiredResponse.builder()
                // ✅ SỬ DỤNG GETTER CHÍNH XÁC: getTwoFactorSessionToken()
                .twoFactorSessionToken(ex.getTwoFactorSessionToken())
                // Giả định message của Exception chứa email hoặc thông tin đăng nhập
                // Nếu bạn cần email, hãy truyền nó qua constructor của Exception từ AuthService
                // Hiện tại, tôi lấy email từ message nếu không có trường email trong Exception
                // (Tốt nhất là thêm trường email vào TwoFactorRequiredException)
                .email(ex.getMessage())
                .build();

        // Trả về 401 Unauthorized (hoặc 400 Bad Request) để Frontend (axios) catch lỗi
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.requiredTwoFactor("2FA required. Please check your email for OTP.", responseData));
    }
}