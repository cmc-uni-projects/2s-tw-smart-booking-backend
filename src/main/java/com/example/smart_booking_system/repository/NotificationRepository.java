package com.example.smart_booking_system.repository;

import com.example.smart_booking_system.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    long countByUserUserIdAndIsReadFalse(String userId);

    List<Notification> findByUserUserIdOrderByCreatedAtDesc(String userId);

    // Đánh dấu TẤT CẢ là đã đọc
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.user.userId = :userId")
    int markAllAsReadByUserId(String userId);

    // Đánh dấu 1 noti là chưa đọc
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = false WHERE n.id = :notificationId AND n.user.userId = :userId")
    int markAsUnreadByUserId(String userId, Long notificationId);

    // Đánh dấu 1 noti là đã đọc
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.id = :notificationId AND n.user.userId = :userId")
    int markAsReadByUserId(String userId, Long notificationId);
    boolean existsByUserUserIdAndTypeAndMessage(String userId, String type, String message);

}
