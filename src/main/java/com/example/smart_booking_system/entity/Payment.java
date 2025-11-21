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
    private String paymentMethod;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(length = 512)
    private String paymentEvidenceUrl;

    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus;

    @Lob
    private String note;

    @Column(nullable = false)
    private LocalDateTime paymentDate;

    private LocalDateTime confirmedDate;

    @Column(columnDefinition = "decimal(10,2)")
    private BigDecimal refundedAmount;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (paymentStatus == null) paymentStatus = PaymentStatus.APPROVED;
    }
}