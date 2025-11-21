package com.example.smart_booking_system.entity;

import com.example.smart_booking_system.enums.DiscountType;
import com.example.smart_booking_system.enums.PromotionStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "promotions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Promotion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer promotionId;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(columnDefinition = "TEXT")
    private String description;

    // --- 1. Loại giảm giá (Logic tính tiền) ---
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DiscountType discountType;

    // --- 2. Trạng thái (Logic hiển thị/quản lý) ---
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PromotionStatus status;

    @Column(nullable = false)
    private BigDecimal discountValue;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    private BigDecimal minBookingAmount;
    private BigDecimal maxDiscountAmount;

    private Integer usageLimit;
    private Integer usageCount = 0;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (usageCount == null) usageCount = 0;

        // Tự động set trạng thái khi tạo mới nếu chưa có
        if (status == null) {
            checkAndSetStatus();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Helper: Tự động chuyển trạng thái dựa vào ngày
    public void checkAndSetStatus() {
        if (this.status == PromotionStatus.PAUSED) return; // Nếu đang Tạm dừng thì giữ nguyên

        LocalDate now = LocalDate.now();
        if (endDate != null && endDate.isBefore(now)) {
            this.status = PromotionStatus.EXPIRED;
        } else {
            this.status = PromotionStatus.ACTIVE;
        }
    }
}