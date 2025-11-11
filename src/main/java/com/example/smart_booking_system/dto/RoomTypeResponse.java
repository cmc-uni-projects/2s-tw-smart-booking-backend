package com.example.smart_booking_system.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class RoomTypeResponse {
    private int roomTypeId;
    private String roomTypeName;
    private String description;
    private BigDecimal pricePerNight;
    private int capacity;
    private String policy;
    private int propertyId;
    private String propertyName;
}
