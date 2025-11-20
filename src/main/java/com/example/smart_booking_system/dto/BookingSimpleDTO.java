package com.example.smart_booking_system.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BookingSimpleDTO {

    private int bookingId;
    private int roomId;
    private int propertyId;
    private String userId;
    private String checkInDate;
    private String checkOutDate;
    private String status;
}
