package com.example.smart_booking_system.dto.response;

import com.example.smart_booking_system.entity.Payment;
import com.example.smart_booking_system.enums.PaymentStatus;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class PaymentResponseDTO {
    private Integer paymentId;
    private int bookingId; // Chỉ lấy ID, không lấy cả object Booking
    private String paymentMethod;
    private BigDecimal amount;
    private String paymentEvidenceUrl;
    private PaymentStatus paymentStatus;
    private String note;
    private LocalDateTime paymentDate;
    private LocalDateTime confirmedDate;

    // Constructor convert từ Entity sang DTO
    public PaymentResponseDTO(Payment payment) {
        this.paymentId = payment.getPaymentId();

        // Quan trọng: Chỉ lấy ID để tránh lỗi Lazy Loading
        if (payment.getBooking() != null) {
            this.bookingId = payment.getBooking().getBookingId();
        }

        this.paymentMethod = payment.getPaymentMethod();
        this.amount = payment.getAmount();
        this.paymentEvidenceUrl = payment.getPaymentEvidenceUrl();
        this.paymentStatus = payment.getPaymentStatus();
        this.note = payment.getNote();
        this.paymentDate = payment.getPaymentDate();
        this.confirmedDate = payment.getConfirmedDate();
    }
}