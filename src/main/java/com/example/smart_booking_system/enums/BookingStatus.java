package com.example.smart_booking_system.enums;

public enum BookingStatus {
    PENDING_PAYMENT,       // Mới tạo, chưa gửi bằng chứng thanh toán
    CONFIRMED,             // Admin đã duyệt tiền
    CANCELLED,             // Hủy
    REFUNDED               // Đã hoàn tiền
}
