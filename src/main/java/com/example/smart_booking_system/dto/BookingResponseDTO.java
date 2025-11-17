package com.example.smart_booking_system.dto;

import com.example.smart_booking_system.enums.BookingStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class BookingResponseDTO {

    private int bookingId;
    private int propertyId;
    private Integer roomId;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private BigDecimal totalPrice;
    private BigDecimal penaltyAmount;
    private BigDecimal refundAmount;
    private BookingStatus status;
}
