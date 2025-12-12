package com.example.smart_booking_system.repository;

import com.example.smart_booking_system.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // 1. Lấy danh sách thông báo của User (Sắp xếp mới nhất trước)
    Page<Notification> findByRecipient_UserIdOrderByCreatedAtDesc(String userId, Pageable pageable);

    // 2. Đếm số lượng chưa đọc
    long countByRecipient_UserIdAndIsReadFalse(String userId);

    // 3. Đánh dấu tất cả là đã đọc
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.recipient.userId = :userId AND n.isRead = false")
    void markAllAsRead(@Param("userId") String userId);
}