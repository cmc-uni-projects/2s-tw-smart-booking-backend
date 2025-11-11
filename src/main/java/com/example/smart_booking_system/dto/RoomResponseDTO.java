package com.example.smart_booking_system.dto;

import com.example.smart_booking_system.enums.RoomCategory;
import com.example.smart_booking_system.enums.RoomStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomResponseDTO {
    private int roomId;
    private String roomName;
    private RoomCategory roomCategory;
    private String description;
    private int capacity;
    private BigDecimal pricePerNight;
    private RoomStatus roomStatus;
    private boolean isActive;
    private int propertyId;
}
