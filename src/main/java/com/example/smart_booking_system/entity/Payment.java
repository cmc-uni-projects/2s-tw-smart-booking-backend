package com.example.smart_booking_system.entity;

import com.example.smart_booking_system.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "Payment")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer paymentId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bookingId", referencedColumnName = "bookingId", unique = true)
    private Booking booking;

    @Column(nullable = false)
    private String paymentMethod; // Ví dụ: "BANK_TRANSFER"

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(length = 512)
    private String paymentEvidenceUrl; // ✅ URL ảnh chụp màn hình chuyển khoản

    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus; // ✅ Trạng thái thanh toán (PENDING, APPROVED, REJECTED)

    @Lob
    private String note; // Ghi chú của Admin (lý do reject...)

    @Column(nullable = false)
    private LocalDateTime paymentDate; // Ngày khách submit

    private LocalDateTime confirmedDate; // Ngày admin duyệt

    @Column(columnDefinition = "decimal(10,2)")
    private BigDecimal refundedAmount; // ✅ THÊM TRƯỜNG NÀY

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (paymentStatus == null) paymentStatus = PaymentStatus.PENDING;
    }
}