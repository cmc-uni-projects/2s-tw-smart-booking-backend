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
    private int paymentId;
    private int bookingId;
    private String propertyName;
    private BigDecimal amount;
    private BigDecimal refundedAmount;
    private String paymentMethod;
    private PaymentStatus paymentStatus;
    private LocalDateTime paymentDate;
    private String note;

    public PaymentResponseDTO(Payment p) {
        this.paymentId = p.getPaymentId();
        this.bookingId = p.getBooking().getBookingId();
        this.propertyName = p.getBooking().getProperty().getPropertyName();
        this.amount = p.getAmount();
        this.refundedAmount = p.getRefundedAmount();
        this.paymentMethod = p.getPaymentMethod();
        this.paymentStatus = p.getPaymentStatus();
        this.paymentDate = p.getPaymentDate();
        this.note = p.getNote();
    }
}