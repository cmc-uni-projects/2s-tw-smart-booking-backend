package com.example.smart_booking_system.dto;

import com.example.smart_booking_system.enums.BookingStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingResponseDTO {

    private int bookingId;
    private int propertyId;
    private Integer roomId;
    private BigDecimal discountAmount;
    private String promotionCode;
    private String propertyName;
    private String propertyAddress;
    private String propertyImage;

    private String roomName;

    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private Integer guestCount;

    private BigDecimal totalPrice;
    private BigDecimal penaltyAmount;
    private BigDecimal refundAmount;

    private BookingStatus status;
    private String paymentStatus;
    private String paymentMethod;
    private String specialRequest;

    private LocalDateTime createdAt;

    private UserSummaryDto user;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserSummaryDto {
        private String userId;
        private String fullName;
        private String email;
        private String phoneNumber;
    }
}