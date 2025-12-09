package com.example.smart_booking_system.controller;

import com.example.smart_booking_system.security.CustomUserDetails;
import com.example.smart_booking_system.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * Đếm số thông báo chưa đọc của user hiện tại
     * GET /api/v1/notifications/unread-count
     */
    @GetMapping("/unread-count")
    public ResponseEntity<?> getUnreadCount(@AuthenticationPrincipal CustomUserDetails currentUser) {
        try {
            if (currentUser == null) {
                System.out.println("⚠️ [unread-count] currentUser = null");
                return ResponseEntity.ok(Collections.singletonMap("count", 0));
            }

            String userId = currentUser.getUserId(); // UUID trong bảng users
            System.out.println("✅ [unread-count] userId = " + userId);

            long count = notificationService.countUnread(userId);
            System.out.println("🔔 [unread-count] userId = " + userId + ", count = " + count);

            return ResponseEntity.ok(Collections.singletonMap("count", count));

        } catch (Exception e) {
            System.err.println("❌ LỖI NGHIÊM TRỌNG TẠI GET /api/v1/notifications/unread-count:");
            e.printStackTrace();
            return ResponseEntity.ok(Collections.singletonMap("count", 0));
        }
    }

    /**
     * Lấy danh sách thông báo của user hiện tại
     * GET /api/v1/notifications
     */
    @GetMapping
    public ResponseEntity<?> getMyNotifications(@AuthenticationPrincipal CustomUserDetails currentUser) {
        try {
            if (currentUser == null) {
                System.out.println("⚠️ [getMyNotifications] currentUser = null");
                return ResponseEntity.ok(Collections.emptyList());
            }

            String userId = currentUser.getUserId();
            System.out.println("✅ [getMyNotifications] userId = " + userId);

            return ResponseEntity.ok(notificationService.getNotificationsForUser(userId));

        } catch (Exception e) {
            System.err.println("❌ LỖI TẠI GET /api/v1/notifications");
            e.printStackTrace();
            return ResponseEntity.ok(Collections.emptyList());
        }
    }

    /**
     * Đánh dấu tất cả thông báo là đã đọc cho user hiện tại
     * PUT /api/v1/notifications/mark-all-read
     */
    @PutMapping("/mark-all-read")
    public ResponseEntity<?> markAllAsRead(@AuthenticationPrincipal CustomUserDetails currentUser) {
        try {
            if (currentUser == null) {
                return ResponseEntity.badRequest().body("User not found");
            }

            String userId = currentUser.getUserId();
            notificationService.markAllAsRead(userId);
            System.out.println("✅ [markAllAsRead] userId = " + userId);

            return ResponseEntity.ok().build();

        } catch (Exception e) {
            System.err.println("❌ LỖI TẠI PUT /api/v1/notifications/mark-all-read");
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Đánh dấu 1 thông báo là chưa đọc
     * PUT /api/v1/notifications/{id}/mark-unread
     */
    @PutMapping("/{id}/mark-unread")
    public ResponseEntity<?> markAsUnread(@PathVariable Long id,
                                          @AuthenticationPrincipal CustomUserDetails currentUser) {
        try {
            if (currentUser == null) {
                return ResponseEntity.badRequest().body("User not found");
            }

            String userId = currentUser.getUserId();
            notificationService.markAsUnread(userId, id);
            System.out.println("✅ [markAsUnread] userId = " + userId + ", notiId = " + id);

            return ResponseEntity.ok().build();

        } catch (Exception e) {
            System.err.println("❌ LỖI TẠI PUT /api/v1/notifications/" + id + "/mark-unread");
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
    // Đánh dấu 1 thông báo là đã đọc
// PUT /api/v1/notifications/{id}/mark-read
    @PutMapping("/{id}/mark-read")
    public ResponseEntity<?> markAsRead(@PathVariable Long id,
                                        @AuthenticationPrincipal CustomUserDetails currentUser) {
        try {
            if (currentUser == null) {
                return ResponseEntity.badRequest().body("User not found");
            }

            String userId = currentUser.getUserId();
            notificationService.markAsRead(userId, id);
            System.out.println("✅ [markAsRead] userId = " + userId + ", notiId = " + id);

            return ResponseEntity.ok().build();

        } catch (Exception e) {
            System.err.println("❌ LỖI TẠI PUT /api/v1/notifications/" + id + "/mark-read");
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Xoá 1 thông báo
     * DELETE /api/v1/notifications/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteNotification(@PathVariable Long id,
                                                @AuthenticationPrincipal CustomUserDetails currentUser) {
        try {
            if (currentUser == null) {
                return ResponseEntity.badRequest().body("User not found");
            }

            String userId = currentUser.getUserId();
            notificationService.deleteNotification(userId, id);
            System.out.println("✅ [deleteNotification] userId = " + userId + ", notiId = " + id);

            return ResponseEntity.noContent().build();

        } catch (Exception e) {
            System.err.println("❌ LỖI TẠI DELETE /api/v1/notifications/" + id);
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
}
