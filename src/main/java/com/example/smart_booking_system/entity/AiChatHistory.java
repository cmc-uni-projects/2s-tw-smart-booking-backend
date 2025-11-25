package com.example.smart_booking_system.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "ai_chat_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiChatHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int chatId;

    // Liên kết với bảng User để biết đoạn chat này của ai
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userId", nullable = false)
    private User userId;

    // Vai trò người gửi: "user" (khách) hoặc "model" (AI)
    // nGoogle Gemini yêu cầu đúng 2 từ khóa này nên mình lưu y nguyê
    @Column(nullable = false, length = 20)
    private String senderRole;

    // Nội dung tin nhắn
    // Dùng columnDefinition = "TEXT" để lưu được đoạn văn dài (ví dụ AI trả lời dài)
    @Column(nullable = false, columnDefinition = "TEXT")
    private String messageContent;

    // Thời gian nhắn
    @Column(nullable = false, updatable = false)
    private LocalDateTime timestamp;

    // Tự động set thời gian khi lưu mới
    @PrePersist
    protected void onCreate() {
        this.timestamp = LocalDateTime.now();
    }
}