package com.example.smart_booking_system.dto.response;

import com.example.smart_booking_system.entity.Payment;
import com.example.smart_booking_system.entity.RefundRequest;
import com.example.smart_booking_system.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class PaymentResponseDTO {
    private int paymentId;
    private int bookingId;

    private String userName;
    private String userEmail;
    private String userPhone;

    private String propertyName;
    private BigDecimal amount;
    private BigDecimal refundedAmount;
    private String paymentMethod;
    private PaymentStatus paymentStatus;
    private LocalDateTime paymentDate;
    private String note;

    // ✅ THÊM MỚI: Thông tin chi tiết yêu cầu hoàn tiền (nếu có)
    private RefundRequestDTO refundRequest;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RefundRequestDTO {
        private String bankName;
        private String accountNumber;
        private String accountHolder;
        private String reason;
        private LocalDateTime requestDate;
    }

    // Constructor cũ (chỉ nhận Payment)
    public PaymentResponseDTO(Payment p) {
        this(p, null); // Gọi constructor mới với refundRequest = null
    }

    // ✅ Constructor MỚI (nhận Payment và RefundRequest)
    public PaymentResponseDTO(Payment p, RefundRequest rr) {
        this.paymentId = p.getPaymentId();

        if (p.getBooking() != null) {
            this.bookingId = p.getBooking().getBookingId();

            // Map Property Name
            if (p.getBooking().getProperty() != null) {
                this.propertyName = p.getBooking().getProperty().getPropertyName();
            }

            // Map User Info
            if (p.getBooking().getUser() != null) {
                this.userName = p.getBooking().getUser().getFullName();
                this.userEmail = p.getBooking().getUser().getEmail();
                this.userPhone = p.getBooking().getUser().getPhoneNumber();
            }
        }

        this.amount = p.getAmount();
        this.refundedAmount = p.getRefundedAmount();
        this.paymentMethod = p.getPaymentMethod();
        this.paymentStatus = p.getPaymentStatus();
        this.paymentDate = p.getPaymentDate();
        this.note = p.getNote();

        // Map thông tin hoàn tiền nếu có
        if (rr != null) {
            this.refundRequest = new RefundRequestDTO(
                    rr.getBankName(),
                    rr.getAccountNumber(),
                    rr.getAccountHolder(),
                    rr.getReason(),
                    rr.getRequestDate()
            );
        }
    }
}