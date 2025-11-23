package com.example.smart_booking_system.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AiAvailableRequest {
    private String city;
    private String checkIn;
    private String checkOut;
}