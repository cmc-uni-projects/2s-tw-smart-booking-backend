package com.example.smart_booking_system.dto.response;

import com.example.smart_booking_system.entity.Payment;
import com.example.smart_booking_system.entity.RefundRequest;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class PaymentResponseDTO {
    private Integer paymentId;
    private int bookingId;
    private String paymentMethod;
    private BigDecimal amount;
    private String transactionReference;
    private String status; // PaymentStatus
    private LocalDateTime paymentDate;
    private String note;

    // Thông tin hoàn tiền (nếu có)
    private RefundInfoDTO refundInfo;

    // Constructor Map từ Entity
    public PaymentResponseDTO(Payment payment, RefundRequest refund) {
        this.paymentId = payment.getPaymentId();
        this.bookingId = payment.getBooking().getBookingId();
        this.paymentMethod = payment.getPaymentMethod();
        this.amount = payment.getTotalAmount();
        this.transactionReference = payment.getTransactionReference();
        this.status = payment.getPaymentStatus().name();
        this.paymentDate = payment.getPaymentDate();
        this.note = payment.getNote();

        if (refund != null) {
            this.refundInfo = new RefundInfoDTO();
            this.refundInfo.setReason(refund.getReason());
            this.refundInfo.setAmount(refund.getAmount());
            this.refundInfo.setRequestDate(refund.getRequestDate());
            this.refundInfo.setStatus(refund.getStatus().name()); // RefundRequestStatus
            this.refundInfo.setAdminNote(refund.getAdminNote());
        }
    }

    @Data
    public static class RefundInfoDTO {
        private String reason;
        private BigDecimal amount;
        private LocalDateTime requestDate;
        private String status;
        private String adminNote;
    }
}