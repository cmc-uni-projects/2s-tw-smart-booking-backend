package com.example.smart_booking_system.entity;

import com.example.smart_booking_system.enums.NotificationType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User recipient; // Người nhận thông báo

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 1000)
    private String message;

    @Enumerated(EnumType.STRING)
    private NotificationType type;

    private boolean isRead = false; // Trạng thái đã đọc/chưa đọc

    private LocalDateTime createdAt;

    // Các trường mở rộng (Optional) để click vào nhảy đến trang chi tiết
    private String relatedEntityId;   // Ví dụ: Booking ID
    private String relatedEntityType; // Ví dụ: "BOOKING", "PROPERTY"

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}