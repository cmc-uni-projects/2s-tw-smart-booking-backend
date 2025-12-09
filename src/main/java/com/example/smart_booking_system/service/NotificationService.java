package com.example.smart_booking_system.service;

import com.example.smart_booking_system.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    // ✅ Sửa kiểu tham số từ Long sang String, và đổi tên hàm gọi repository
    public long countUnread(String userId) {
        return notificationRepository.countByUserUserIdAndIsReadFalse(userId);
    }
}