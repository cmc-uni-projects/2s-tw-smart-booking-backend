package com.example.smart_booking_system.enums;

public enum PaymentStatus {
    PENDING,            // Chờ thanh toán
    APPROVED,           // Đã thanh toán thành công
    REJECTED,           // Thanh toán thất bại
    REFUND_REQUESTED,   // Khách đã gửi yêu cầu hoàn tiền
    REFUNDED            // Admin đã hoàn tiền xong
}