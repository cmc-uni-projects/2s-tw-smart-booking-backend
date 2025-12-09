package com.example.smart_booking_system.controller;

import com.example.smart_booking_system.service.NotificationService;
import com.example.smart_booking_system.repository.UserRepository;
import com.example.smart_booking_system.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.Collections;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/notifications") // ✅ Đã thêm /v1
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final UserRepository userRepository;

    @GetMapping("/unread-count")
    public ResponseEntity<?> getUnreadCount(@AuthenticationPrincipal Object principal) {

        String identifier = null;
        String actualUserId = null;

        try {
            // 1. Lấy định danh từ Token
            if (principal instanceof Authentication) {
                Authentication auth = (Authentication) principal;
                identifier = auth.getName();
            }

            // 2. Tra cứu ID User từ DB
            if (identifier != null) {
                // Ưu tiên tìm bằng Email
                Optional<User> userOpt = userRepository.findByEmail(identifier);
                if (userOpt.isPresent()) {
                    actualUserId = userOpt.get().getUserId();
                } else {
                    // Nếu không thấy, thử tìm bằng ID trực tiếp
                    Optional<User> userByIdOpt = userRepository.findById(identifier);
                    if (userByIdOpt.isPresent()) {
                        actualUserId = userByIdOpt.get().getUserId();
                    }
                }
            }

            // Nếu không tìm thấy User -> Trả về 0
            if (actualUserId == null) {
                return ResponseEntity.ok(Collections.singletonMap("count", 0));
            }

            // 3. Gọi Service (Bọc trong try-catch để xem lỗi SQL nếu có)
            long count = notificationService.countUnread(actualUserId);
            return ResponseEntity.ok(Collections.singletonMap("count", count));

        } catch (Exception e) {
            // 🔥 IN LỖI RA CONSOLE ĐỂ DEBUG
            System.err.println("❌ LỖI NGHIÊM TRỌNG TẠI GET /unread-count:");
            e.printStackTrace(); // In toàn bộ stack trace lỗi

            // Trả về 0 để Frontend không bị lỗi 500
            return ResponseEntity.ok(Collections.singletonMap("count", 0));
        }
    }
}