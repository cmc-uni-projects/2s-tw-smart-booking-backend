package com.example.smart_booking_system.dto.request;

import lombok.Data;

import java.time.LocalDate;

@Data
public class BookingRequestDTO {

    private String userId; // UUID string
    private int propertyId;
    private Integer roomId; // optional: null when booking whole property

    private LocalDate checkInDate;
    private LocalDate checkOutDate;
}
