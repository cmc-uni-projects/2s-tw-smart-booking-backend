package com.example.smart_booking_system.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AiBookingRequest {
    private Integer roomId;
    private String checkIn;
    private String checkOut;
    private String userId;
}
