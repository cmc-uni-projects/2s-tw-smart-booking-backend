package com.example.smart_booking_system.repository;

import com.example.smart_booking_system.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    long countByUserUserIdAndIsReadFalse(String userId);

    List<Notification> findByUserUserIdOrderByCreatedAtDesc(String userId);
}