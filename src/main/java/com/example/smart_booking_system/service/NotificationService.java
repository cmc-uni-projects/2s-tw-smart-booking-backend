package com.example.smart_booking_system.service;

import com.example.smart_booking_system.dto.response.notification.NotificationResponseDTO;
import com.example.smart_booking_system.entity.Notification;
import com.example.smart_booking_system.entity.User;
import com.example.smart_booking_system.enums.NotificationType;
import com.example.smart_booking_system.repository.NotificationRepository;
import com.example.smart_booking_system.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepo;
    private final UserRepository userRepo;

    // --- PHẦN 1: API CHO CONTROLLER ---

    // Lấy danh sách (trả về cả số lượng chưa đọc trong cùng 1 lần gọi cho tiện)
    public Map<String, Object> getUserNotifications(String userId, Pageable pageable) {
        Page<Notification> page = notificationRepo.findByRecipient_UserIdOrderByCreatedAtDesc(userId, pageable);
        long unreadCount = notificationRepo.countByRecipient_UserIdAndIsReadFalse(userId);

        Page<NotificationResponseDTO> dtoPage = page.map(this::convertToDTO);

        Map<String, Object> result = new HashMap<>();
        result.put("content", dtoPage.getContent());
        result.put("totalPages", dtoPage.getTotalPages());
        result.put("unreadCount", unreadCount);

        return result;
    }

    public long getUnreadCount(String userId) {
        return notificationRepo.countByRecipient_UserIdAndIsReadFalse(userId);
    }

    @Transactional
    public void markAsRead(Long notificationId, String userId) {
        Notification notification = notificationRepo.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));

        if (!notification.getRecipient().getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized access to notification");
        }

        notification.setRead(true);
        notificationRepo.save(notification);
    }

    @Transactional
    public void markAllAsRead(String userId) {
        notificationRepo.markAllAsRead(userId);
    }

    // --- PHẦN 2: HÀM NỘI BỘ (Để các Service khác gọi khi muốn bắn thông báo) ---

    @Transactional
    public void sendNotification(User recipient, String title, String message, NotificationType type, String relatedId) {
        if (recipient == null) return;

        Notification notification = Notification.builder()
                .recipient(recipient)
                .title(title)
                .message(message)
                .type(type)
                .relatedEntityId(relatedId)
                .isRead(false)
                .build();

        notificationRepo.save(notification);
        // TODO: Nếu có WebSocket, bắn event real-time tại đây
    }

    private NotificationResponseDTO convertToDTO(Notification n) {
        return NotificationResponseDTO.builder()
                .id(n.getId())
                .title(n.getTitle())
                .message(n.getMessage())
                .type(n.getType())
                .isRead(n.isRead())
                .createdAt(n.getCreatedAt())
                .relatedEntityId(n.getRelatedEntityId())
                .build();
    }
}