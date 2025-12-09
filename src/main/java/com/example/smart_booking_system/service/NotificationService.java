package com.example.smart_booking_system.service;

import com.example.smart_booking_system.entity.Notification;
import com.example.smart_booking_system.repository.NotificationRepository;
import com.example.smart_booking_system.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public long countUnread(String userId) {
        long count = notificationRepository.countByUserUserIdAndIsReadFalse(userId);
        System.out.println("🔔 [Service.countUnread] userId = " + userId + ", count = " + count);
        return count;
    }

    @Transactional(readOnly = true)
    public List<Notification> getNotificationsForUser(String userId) {
        List<Notification> list = notificationRepository.findByUserUserIdOrderByCreatedAtDesc(userId);
        System.out.println("📨 [Service.getNotificationsForUser] userId = " + userId + ", size = " + list.size());
        return list;
    }

    public void markAllAsRead(String userId) {
        int updated = notificationRepository.markAllAsReadByUserId(userId);
        System.out.println("✅ [Service.markAllAsRead] userId = " + userId + ", updated = " + updated);
    }

    public void markAsUnread(String userId, Long notificationId) {
        int updated = notificationRepository.markAsUnreadByUserId(userId, notificationId);
        System.out.println("✅ [Service.markAsUnread] userId = " + userId +
                ", notiId = " + notificationId + ", updated = " + updated);
    }

    public void markAsRead(String userId, Long notificationId) {
        int updated = notificationRepository.markAsReadByUserId(userId, notificationId);
        System.out.println("✅ [Service.markAsRead] userId = " + userId +
                ", notiId = " + notificationId + ", updated = " + updated);
    }

    public void deleteNotification(String userId, Long notificationId) {
        Notification noti = notificationRepository.findById(notificationId).orElse(null);
        if (noti == null) {
            System.out.println("⚠️ [Service.deleteNotification] not found, notiId = " + notificationId);
            return;
        }

        if (noti.getUser() != null && userId.equals(noti.getUser().getUserId())) {
            notificationRepository.delete(noti);
            System.out.println("🗑 [Service.deleteNotification] userId = " + userId +
                    ", notiId = " + notificationId);
        } else {
            System.out.println("⚠️ [Service.deleteNotification] not owner, userId = " + userId +
                    ", noti.userId = " + (noti.getUser() != null ? noti.getUser().getUserId() : "null"));
        }
    }
}
