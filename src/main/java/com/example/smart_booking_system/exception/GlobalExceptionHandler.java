package com.example.smart_booking_system.exception;

import com.example.smart_booking_system.dto.response.ApiResponse;
import com.example.smart_booking_system.i18n.Messages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Turns exceptions into ApiResponse errors in the request language (Accept-Language).
 * Business errors carry an i18n key (see AppException); unexpected errors are logged once with
 * their stack trace and answered with a generic message (no internal details leak to clients).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final Messages messages;

    public GlobalExceptionHandler(Messages messages) {
        this.messages = messages;
    }

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ApiResponse<?>> handleAppException(AppException ex, WebRequest request) {
        return error(ex.getStatus(), ex.getMessageKey(), messages.t(ex.getMessageKey(), ex.getArgs()), request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<?>> handleAccessDenied(AccessDeniedException ex, WebRequest request) {
        return error(HttpStatus.FORBIDDEN, "error.accessDenied", messages.t("error.accessDenied"), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        // Default messages are already interpolated from messages*.properties ({key}) in the request locale
        ex.getBindingResult().getAllErrors().forEach(e ->
                errors.put(e instanceof FieldError fe ? fe.getField() : e.getObjectName(), e.getDefaultMessage()));
        ApiResponse<Map<String, String>> response = new ApiResponse<>();
        response.setSuccess(false);
        response.setMessage(messages.t("error.validation"));
        response.setCode("error.validation");
        response.setData(errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<?>> handleBadCredentials(BadCredentialsException ex, WebRequest request) {
        return error(HttpStatus.UNAUTHORIZED, "auth.badCredentials", messages.t("auth.badCredentials"), request);
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ApiResponse<?>> handleDisabled(DisabledException ex, WebRequest request) {
        return error(HttpStatus.FORBIDDEN, "auth.notVerified", messages.t("auth.notVerified"), request);
    }

    @ExceptionHandler(LockedException.class)
    public ResponseEntity<ApiResponse<?>> handleLocked(LockedException ex, WebRequest request) {
        return error(HttpStatus.FORBIDDEN, "auth.locked", messages.t("auth.locked"), request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<?>> handleUnexpected(Exception ex, WebRequest request) {
        log.error("Unhandled exception on {}", request.getDescription(false), ex);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "error.unexpected", messages.t("error.unexpected"), request);
    }

    private static ResponseEntity<ApiResponse<?>> error(HttpStatus status, String code, String message, WebRequest request) {
        ApiResponse<?> body = ApiResponse.error(message, request.getDescription(false));
        body.setCode(code);
        return ResponseEntity.status(status).body(body);
    }
}
