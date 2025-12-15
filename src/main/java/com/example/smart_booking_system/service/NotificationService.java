package com.example.smart_booking_system.service;

import com.example.smart_booking_system.dto.response.notification.NotificationResponseDTO;
import com.example.smart_booking_system.entity.Notification;
import com.example.smart_booking_system.entity.User;
import com.example.smart_booking_system.enums.NotificationType;
import com.example.smart_booking_system.exception.ResourceNotFoundException;
import com.example.smart_booking_system.repository.NotificationRepository;
import com.example.smart_booking_system.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    // --- ĐỊNH NGHĨA SCOPE (Giữ nguyên) ---
    private static final List<NotificationType> CUSTOMER_TYPES = Arrays.asList(
            NotificationType.GENERAL, NotificationType.ACCOUNT_UPDATE, NotificationType.SECURITY_ALERT,
            NotificationType.BOOKING_SUCCESS, NotificationType.BOOKING_CANCELLED, NotificationType.BOOKING_FAILED,
            NotificationType.PAYMENT_SUCCESS, NotificationType.REFUND_PROCESSED, NotificationType.PROMOTION,
            NotificationType.REMINDER_CHECKIN, NotificationType.APPROVAL, NotificationType.REJECTION
    );

    private static final List<NotificationType> OWNER_TYPES = Arrays.asList(
            NotificationType.BOOKING_RECEIVED, NotificationType.BOOKING_CANCELLED_BY_GUEST,
            NotificationType.REVENUE_REPORT, NotificationType.PROPERTY_SUSPENDED, NotificationType.PAYOUT_SUCCESS,
            NotificationType.ROOM_SUSPENDED,
            NotificationType.SYSTEM
    );

    // --- PHẦN 1: LOGIC ĐỌC (GET) ---

    // [SỬA] Long userId -> String userId
    public Page<NotificationResponseDTO> getUserNotifications(String userId, String scope, Pageable pageable) {
        List<NotificationType> targetTypes = getTypesByScope(scope);
        return notificationRepository.findByUserIdAndTypeInOrderByCreatedAtDesc(userId, targetTypes, pageable)
                .map(this::convertToDTO);
    }

    // [SỬA] Long userId -> String userId
    public long getUnreadCount(String userId, String scope) {
        return notificationRepository.countByUserIdAndIsReadFalseAndTypeIn(userId, getTypesByScope(scope));
    }

    // [SỬA] Long userId -> String userId
    @Transactional
    public void markAllAsRead(String userId, String scope) {
        notificationRepository.markAllAsReadByType(userId, getTypesByScope(scope));
    }

    // [SỬA] Long userId -> String userId
    @Transactional
    public void markAsRead(Long id, String userId) {
        // Kiểm tra tồn tại
        if (!notificationRepository.existsById(id)) {
            throw new ResourceNotFoundException("Notification not found");
        }
        notificationRepository.markAsRead(id, userId);
    }

    // --- PHẦN 2: LOGIC GHI (CREATE/SEND) ---

    // [SỬA] Long userId -> String userId
    // Đây là chỗ gây ra lỗi biên dịch trước đó
    @Transactional
    public void sendNotification(String userId, String title, String message, NotificationType type, String relatedEntityId) {
        // findById giờ nhận String, khớp với User entity
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found to send notification"));

        Notification notification = Notification.builder()
                .user(user)
                .title(title)
                .message(message)
                .type(type)
                .isRead(false)
                .relatedEntityId(relatedEntityId)
                .build();

        notificationRepository.save(notification);
    }

    private List<NotificationType> getTypesByScope(String scope) {
        if ("OWNER".equalsIgnoreCase(scope)) return OWNER_TYPES;
        if ("CUSTOMER".equalsIgnoreCase(scope)) return CUSTOMER_TYPES;
        return Arrays.asList(NotificationType.values());
    }

    private NotificationResponseDTO convertToDTO(Notification notification) {
        return NotificationResponseDTO.builder()
                .id(notification.getId())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .type(notification.getType())
                .isRead(notification.isRead())
                .createdAt(notification.getCreatedAt())
                .relatedEntityId(notification.getRelatedEntityId())
                .build();
    }
}