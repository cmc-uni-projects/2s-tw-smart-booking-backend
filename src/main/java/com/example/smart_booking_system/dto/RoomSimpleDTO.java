package com.example.smart_booking_system.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class RoomSimpleDTO {
    private int roomId;
    private String roomName;
    private int capacity;
    private BigDecimal pricePerNight;
    private String roomStatus;
    private boolean isActive;
}
