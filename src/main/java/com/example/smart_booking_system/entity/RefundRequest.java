package com.example.smart_booking_system.entity;

import com.example.smart_booking_system.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "refund_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefundRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paymentId")
    private Payment payment; // Liên kết 1-1 với Payment

    // Thông tin người nhận tiền
    private String bankName;
    private String accountNumber;
    private String accountHolder;

    @Column(columnDefinition = "TEXT")
    private String reason; // Lý do khách nhập

    @Column(columnDefinition = "TEXT")
    private String adminNote; // Ghi chú của admin khi duyệt/từ chối

    private LocalDateTime requestDate;
    private LocalDateTime processDate; // Ngày admin xử lý

    @Enumerated(EnumType.STRING)
    private PaymentStatus status; // REFUND_REQUESTED, REFUNDED, REJECTED

    @PrePersist
    protected void onCreate() {
        requestDate = LocalDateTime.now();
        status = PaymentStatus.REFUND_REQUESTED;
    }
}